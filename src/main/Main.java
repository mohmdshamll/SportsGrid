package main;

import model.Admin;
import model.Booking;
import model.CardPayment;
import model.Player;
import model.Review;
import model.Role;
import model.TimeSlot;
import model.Turf;
import model.TurfOwner;
import model.UPIPayment;
import model.User;
import service.AdminService;
import service.AuthService;
import service.BookingService;
import service.PaymentFailedException;
import service.TurfService;

import java.time.LocalDate;
import java.util.List;

/**
 * BACKEND DEMO (console) and Swing UI Launcher.
 */
public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--demo")) {
            runBackendDemo();
        } else {
            // Apply font rendering hints and dark theme defaults before any Swing component initializes
            ui.StyleConstants.applyGlobalDefaults();

            // Verify or safely initialize default administrator account during normal startup
            new AuthService().initializeDefaultAdmin();

            javax.swing.SwingUtilities.invokeLater(() -> new ui.LoginFrame().setVisible(true));
        }
    }

    public static void runBackendDemo() {
        AuthService auth = new AuthService();
        TurfService turfService = new TurfService();
        BookingService bookingService = new BookingService();
        AdminService adminService = new AdminService();

        // unique emails on every run, so the demo can be run again and again
        String tag = String.valueOf(System.currentTimeMillis() % 1_000_000);

        step("1. Accounts");
        auth.seedAdminIfMissing("admin@sportgrid.com", "Admin@123");   // demo credentials, change for real use
        User admin = auth.login("admin@sportgrid.com", "Admin@123");
        TurfOwner owner = (TurfOwner) auth.register("Ravi Owner", "owner" + tag + "@test.com", "9876543210", "owner123", Role.OWNER);
        Player asha = (Player) auth.register("Asha Player", "asha" + tag + "@test.com", "9876500001", "asha1234", Role.PLAYER);
        Player kiran = (Player) auth.register("Kiran Player", "kiran" + tag + "@test.com", "9876500002", "kiran123", Role.PLAYER);
        System.out.println(admin.getDashboard());
        System.out.println(owner.getDashboard());
        System.out.println(asha.getDashboard());

        step("2. Owner adds a turf, admin approves it");
        Turf turf = turfService.addTurf(owner, "Green Arena " + tag, "Kakkanad, Kochi", "Football", 1200);
        System.out.println("Pending turfs for admin: " + adminService.getPendingTurfs(admin).size() + " (includes ours)");
        adminService.approveTurf(admin, turf.getTurfId());

        step("3. Owner creates slots for tomorrow (18:00 to 22:00)");
        int created = turfService.generateHourlySlots(owner, turf.getTurfId(), LocalDate.now().plusDays(1), 18, 22);
        System.out.println("Slots created: " + created);

        step("4. Player searches and views free slots");
        List<Turf> found = bookingService.searchTurfs("Football", "Kochi");
        System.out.println("Search found " + found.size() + " turf(s), first: " + found.get(0));
        List<TimeSlot> slots = bookingService.getAvailableSlots(turf.getTurfId());
        slots.forEach(s -> System.out.println("  free: " + s));

        step("5. Asha books slot 1 with UPI");
        Booking booking = bookingService.bookSlot(asha, slots.get(0).getSlotId(), new UPIPayment("asha@okhdfc"));
        System.out.println("Booked: " + booking + " | paid with " + booking.getPayment().getMethod());

        step("6. Kiran tries the SAME slot (must be refused)");
        try {
            bookingService.bookSlot(kiran, slots.get(0).getSlotId(), new UPIPayment("kiran@okicici"));
            System.out.println("ERROR: double booking was allowed!");
        } catch (IllegalStateException e) {
            System.out.println("Refused as expected: " + e.getMessage());
        }

        step("7. Kiran pays with a wrong card number (payment fails, slot must be freed)");
        try {
            bookingService.bookSlot(kiran, slots.get(1).getSlotId(), new CardPayment("Kiran", "1234 5678 9012 3456", "12/30"));
            System.out.println("ERROR: bad card was accepted!");
        } catch (PaymentFailedException e) {
            System.out.println("Refused as expected: " + e.getMessage());
        }
        System.out.println("Free slots now: " + bookingService.getAvailableSlots(turf.getTurfId()).size() + " (should be 3)");

        step("8. Kiran retries with a valid test card");
        Booking b2 = bookingService.bookSlot(kiran, slots.get(1).getSlotId(), new CardPayment("Kiran", "4111 1111 1111 1111", "12/30"));
        System.out.println("Booked: " + b2 + " | card ending " + b2.getPayment().getReference());

        step("9. Owner dashboard");
        turfService.loadOwnerDashboard(owner);
        System.out.println(owner.getDashboard() + " (expected Rs 2400.00)");
        turfService.getOwnerBookings(owner).forEach(b -> System.out.println("  " + b));

        step("10. Asha cancels her booking, the slot becomes free and earnings drop");
        bookingService.cancelBooking(asha, booking.getBookingId());
        turfService.loadOwnerDashboard(owner);
        System.out.println(owner.getDashboard() + " (expected Rs 1200.00)");
        System.out.println("Free slots now: " + bookingService.getAvailableSlots(turf.getTurfId()).size() + " (should be 3)");

        step("11. Reviews");
        Review review = turfService.addReview(kiran, turf.getTurfId(), 5, "Great pitch and lights!");
        System.out.println("Added: " + review);
        try {
            turfService.addReview(asha, turf.getTurfId(), 4, "Nice");   // her booking is cancelled
            System.out.println("ERROR: review without a confirmed booking was allowed!");
        } catch (IllegalStateException e) {
            System.out.println("Refused as expected: " + e.getMessage());
        }
        System.out.println("Average rating: " + bookingService.searchTurfs("Football", "Kochi").get(0).getAverageRating());

        step("12. History and dashboards");
        bookingService.getBookingHistory(asha).forEach(b -> System.out.println("  " + b));
        System.out.println(asha.getDashboard());

        System.out.println("\nAll backend checks finished.");
    }

    private static void step(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
