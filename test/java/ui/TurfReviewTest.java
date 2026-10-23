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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TurfReviewTest {

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
    @DisplayName("Verify rating submission, average calculation, duplicate guard, and booking eligibility")
    void testTurfReviewWorkflow() {
        String tag = String.valueOf(System.currentTimeMillis() % 1_000_000);

        // 1. Create Owner & Approved Turf
        TurfOwner owner = (TurfOwner) authService.register("Review Owner " + tag, "rev_owner_" + tag + "@test.com", "9876543201", "pass123", Role.OWNER);
        Turf turf = turfService.addTurf(owner, "Review Arena " + tag, "Kochi", "Football", 1200);
        adminService.approveTurf(admin, turf.getTurfId());

        // 2. Generate future slots
        LocalDate tomorrow = LocalDate.now().plusDays(2);
        int created = turfService.generateHourlySlots(owner, turf.getTurfId(), tomorrow, 16, 20);
        assertTrue(created >= 3, "Should have created slots");

        List<TimeSlot> slots = bookingService.getAvailableSlots(turf.getTurfId());
        int slot1Id = slots.get(0).getSlotId();
        int slot2Id = slots.get(1).getSlotId();
        int slot3Id = slots.get(2).getSlotId();

        // 3. Register players
        Player player1 = (Player) authService.register("Reviewer One", "p1_" + tag + "@test.com", "9876100001", "pass123", Role.PLAYER);
        Player player2 = (Player) authService.register("Reviewer Two", "p2_" + tag + "@test.com", "9876100002", "pass123", Role.PLAYER);
        Player unbookedPlayer = (Player) authService.register("Unbooked Player", "unbooked_" + tag + "@test.com", "9876100003", "pass123", Role.PLAYER);

        // 4. UNAUTHORIZED CHECK: Unbooked player tries to review -> MUST FAIL
        assertThrows(IllegalStateException.class, () -> {
            turfService.addReview(unbookedPlayer, turf.getTurfId(), 5, "Pitch looks great from outside!");
        }, "Player without confirmed booking must be rejected");

        // 5. VALIDATION CHECKS
        // Rating out of range (< 1 or > 5)
        assertThrows(IllegalArgumentException.class, () -> {
            turfService.addReview(player1, turf.getTurfId(), 0, "Zero stars");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            turfService.addReview(player1, turf.getTurfId(), 6, "Six stars");
        });

        // Comment > 500 characters
        String longComment = "a".repeat(501);
        assertThrows(IllegalArgumentException.class, () -> {
            turfService.addReview(player1, turf.getTurfId(), 5, longComment);
        });

        // 6. Player 1 books slot 1
        Booking b1 = bookingService.bookSlot(player1, slot1Id, new UPIPayment("p1@upi"));
        assertNotNull(b1);
        assertEquals(BookingStatus.CONFIRMED, b1.getStatus());

        // Verify getBookedTurfs includes this turf for Player 1
        List<Turf> bookedByP1 = bookingService.getBookedTurfs(player1);
        assertTrue(bookedByP1.stream().anyMatch(t -> t.getTurfId() == turf.getTurfId()), "Turf must appear in player's booked turfs");

        // Before review, hasReviewed must be false
        assertFalse(turfService.hasReviewed(player1, turf.getTurfId()));

        // 7. Player 1 submits valid 5-star review
        Review r1 = turfService.addReview(player1, turf.getTurfId(), 5, "Superb turf, great floodlights and turf grass quality!");
        assertNotNull(r1);
        assertEquals(5, r1.getRating());
        assertEquals(player1.getUserId(), r1.getPlayerId());
        assertEquals(turf.getTurfId(), r1.getTurfId());

        // Now hasReviewed must be true
        assertTrue(turfService.hasReviewed(player1, turf.getTurfId()));

        // getPlayerReview should return the review
        Optional<Review> optRev = turfService.getPlayerReview(player1, turf.getTurfId());
        assertTrue(optRev.isPresent());
        assertEquals(5, optRev.get().getRating());

        // 8. DUPLICATE CHECK: Player 1 tries to review the same turf again -> MUST FAIL
        assertThrows(IllegalStateException.class, () -> {
            turfService.addReview(player1, turf.getTurfId(), 4, "Trying to review again");
        }, "Duplicate review for same turf must be rejected");

        // 9. Verify average rating updated in DB
        List<Turf> searchResult1 = bookingService.searchTurfs("Football", "Kochi");
        Turf foundTurf1 = searchResult1.stream().filter(t -> t.getTurfId() == turf.getTurfId()).findFirst().orElse(null);
        assertNotNull(foundTurf1);
        assertEquals(5.0, foundTurf1.getAverageRating(), 0.01, "Average rating should be 5.0 after 1 five-star review");

        // 10. Player 2 books slot 2 and leaves a 3-star review
        Booking b2 = bookingService.bookSlot(player2, slot2Id, new UPIPayment("p2@okhdfc"));
        assertNotNull(b2);

        Review r2 = turfService.addReview(player2, turf.getTurfId(), 3, "Decent pitch, but parking was crowded.");
        assertNotNull(r2);

        // Verify new average rating: (5.0 + 3.0) / 2 = 4.0
        List<Turf> searchResult2 = bookingService.searchTurfs("Football", "Kochi");
        Turf foundTurf2 = searchResult2.stream().filter(t -> t.getTurfId() == turf.getTurfId()).findFirst().orElse(null);
        assertNotNull(foundTurf2);
        assertEquals(4.0, foundTurf2.getAverageRating(), 0.01, "Average rating should be 4.0 after 5 and 3 star reviews");

        // 11. CANCELLED BOOKING ELIGIBILITY CHECK:
        // Player 3 books slot 3, then cancels it. Attempting to review without any confirmed booking must fail.
        Player player3 = (Player) authService.register("Cancel Player", "p3_" + tag + "@test.com", "9876100004", "pass123", Role.PLAYER);
        Booking b3 = bookingService.bookSlot(player3, slot3Id, new UPIPayment("p3@upi"));
        bookingService.cancelBooking(player3, b3.getBookingId());

        assertThrows(IllegalStateException.class, () -> {
            turfService.addReview(player3, turf.getTurfId(), 4, "Cancelled but trying to review");
        }, "Player whose booking was cancelled must not be allowed to review");
    }
}
