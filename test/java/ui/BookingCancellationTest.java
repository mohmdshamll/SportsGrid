package ui;

import model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import service.AdminService;
import service.AuthService;
import service.BookingService;
import service.TurfService;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookingCancellationTest {

    private static AuthService authService;
    private static TurfService turfService;
    private static BookingService bookingService;
    private static AdminService adminService;
    private static User admin;

    @BeforeAll
    static void setup() {
        authService = new AuthService();
        turfService = new TurfService();
        bookingService = new BookingService();
        adminService = new AdminService();

        authService.seedAdminIfMissing("admin@sportgrid.com", "Admin@123");
        admin = authService.login("admin@sportgrid.com", "Admin@123");
    }

    @Test
    @DisplayName("Verify full booking cancellation, slot release, refund status, and security checks")
    void testBookingCancellationWorkflow() {
        String tag = String.valueOf(System.currentTimeMillis() % 1_000_000);

        // 1. Create Owner & Turf
        TurfOwner owner = (TurfOwner) authService.register("Cancel Test Owner", "owner_cancel_" + tag + "@test.com", "9876543210", "pass123", Role.OWNER);
        Turf turf = turfService.addTurf(owner, "Cancel Arena " + tag, "Kochi", "Football", 1000);
        adminService.approveTurf(admin, turf.getTurfId());

        // 2. Generate future slots
        LocalDate tomorrow = LocalDate.now().plusDays(2);
        int created = turfService.generateHourlySlots(owner, turf.getTurfId(), tomorrow, 18, 20);
        assertTrue(created >= 2, "Should have created at least 2 slots");

        List<TimeSlot> availableSlots = bookingService.getAvailableSlots(turf.getTurfId());
        TimeSlot targetSlot = availableSlots.get(0);
        int slotId = targetSlot.getSlotId();

        // 3. Register Player A and Player B
        Player playerA = (Player) authService.register("Player A", "player_a_" + tag + "@test.com", "9876500001", "pass123", Role.PLAYER);
        Player playerB = (Player) authService.register("Player B", "player_b_" + tag + "@test.com", "9876500002", "pass123", Role.PLAYER);

        // 4. Player A books slot with UPI
        Booking booking = bookingService.bookSlot(playerA, slotId, new UPIPayment("playera@upi"));
        assertNotNull(booking);
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(PaymentStatus.SUCCESS, booking.getPayment().getStatus());

        // Verify slot is booked in DB
        List<TimeSlot> slotsAfterBooking = bookingService.getAvailableSlots(turf.getTurfId());
        assertFalse(slotsAfterBooking.stream().anyMatch(s -> s.getSlotId() == slotId), "Booked slot must not be in available slots");

        // 5. SECURITY CHECK: Player B tries to cancel Player A's booking -> MUST FAIL
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.cancelBooking(playerB, booking.getBookingId());
        }, "Player B must not be allowed to cancel Player A's booking");

        // 6. Player A cancels their booking
        assertDoesNotThrow(() -> {
            bookingService.cancelBooking(playerA, booking.getBookingId());
        });

        // 7. Verify slot is released and back in available slots
        List<TimeSlot> slotsAfterCancel = bookingService.getAvailableSlots(turf.getTurfId());
        assertTrue(slotsAfterCancel.stream().anyMatch(s -> s.getSlotId() == slotId), "Cancelled slot must be returned to available slots");

        // 8. Verify booking history for Player A shows CANCELLED status and REFUNDED payment
        List<Booking> history = bookingService.getBookingHistory(playerA);
        Booking cancelledBooking = history.stream().filter(b -> b.getBookingId() == booking.getBookingId()).findFirst().orElse(null);
        assertNotNull(cancelledBooking);
        assertEquals(BookingStatus.CANCELLED, cancelledBooking.getStatus());
        assertEquals(PaymentStatus.REFUNDED, cancelledBooking.getPayment().getStatus());

        // 9. RE-CANCELLATION GUARD: Trying to cancel already-cancelled booking must fail
        assertThrows(IllegalStateException.class, () -> {
            bookingService.cancelBooking(playerA, booking.getBookingId());
        }, "Cancelling an already-cancelled booking must throw IllegalStateException");
    }
}
