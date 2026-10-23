package service;

import dao.BookingDAO;
import dao.ReviewDAO;
import dao.TurfDAO;
import dao.UserDAO;
import model.Admin;
import model.Booking;
import model.Turf;
import model.User;

import java.util.List;

/** Admin-only actions. Every method first checks that the caller really is an Admin. */
public class AdminService {

    private final UserDAO userDAO = new UserDAO();
    private final TurfDAO turfDAO = new TurfDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();

    public static class AdminSummary {
        private final int totalUsers;
        private final int totalTurfs;
        private final int pendingTurfs;
        private final int totalBookings;
        private final double totalRevenue;

        public AdminSummary(int totalUsers, int totalTurfs, int pendingTurfs, int totalBookings, double totalRevenue) {
            this.totalUsers = totalUsers;
            this.totalTurfs = totalTurfs;
            this.pendingTurfs = pendingTurfs;
            this.totalBookings = totalBookings;
            this.totalRevenue = totalRevenue;
        }

        public int getTotalUsers() { return totalUsers; }
        public int getTotalTurfs() { return totalTurfs; }
        public int getPendingTurfs() { return pendingTurfs; }
        public int getTotalBookings() { return totalBookings; }
        public double getTotalRevenue() { return totalRevenue; }
    }

    public AdminSummary getSummary(User caller) {
        requireAdmin(caller);
        int users = userDAO.findAll().size();
        List<Turf> allTurfs = turfDAO.findAll();
        int pending = (int) allTurfs.stream().filter(t -> !t.isApproved()).count();
        List<Booking> allBookings = bookingDAO.findAll();
        double revenue = bookingDAO.sumAllConfirmed();
        return new AdminSummary(users, allTurfs.size(), pending, allBookings.size(), revenue);
    }

    public List<User> listUsers(User caller) {
        requireAdmin(caller);
        return userDAO.findAll();
    }

    /** Deletes a user and (by CASCADE) their turfs, bookings and reviews. Admins cannot delete themselves. */
    public void removeUser(User caller, int userId) {
        requireAdmin(caller);
        if (caller.getUserId() == userId) throw new IllegalArgumentException("You cannot remove your own account");
        if (!userDAO.delete(userId)) throw new IllegalArgumentException("User not found");
    }

    public List<Turf> getAllTurfs(User caller) {
        requireAdmin(caller);
        return turfDAO.findAll();
    }

    public List<Turf> getPendingTurfs(User caller) {
        requireAdmin(caller);
        return turfDAO.findPending();
    }

    public void approveTurf(User caller, int turfId) {
        requireAdmin(caller);
        turfDAO.findById(turfId).orElseThrow(() -> new IllegalArgumentException("Turf not found"));
        turfDAO.setApproved(turfId, true);
    }

    /** Rejecting deletes a turf that was never approved. Approved turfs are not removed here. */
    public void rejectTurf(User caller, int turfId) {
        requireAdmin(caller);
        Turf t = turfDAO.findById(turfId).orElseThrow(() -> new IllegalArgumentException("Turf not found"));
        if (t.isApproved()) throw new IllegalStateException("This turf is already approved");
        turfDAO.delete(turfId);
    }

    /** Deletes an existing turf (e.g. inactive or rule-violating) with all its slots and bookings via CASCADE. */
    public void deleteTurf(User caller, int turfId) {
        requireAdmin(caller);
        turfDAO.findById(turfId).orElseThrow(() -> new IllegalArgumentException("Turf not found"));
        turfDAO.delete(turfId);
    }

    public List<Booking> getAllBookings(User caller) {
        requireAdmin(caller);
        return bookingDAO.findAll();
    }

    public void removeReview(User caller, int reviewId) {
        requireAdmin(caller);
        if (!reviewDAO.delete(reviewId)) throw new IllegalArgumentException("Review not found");
    }

    private void requireAdmin(User caller) {
        if (!(caller instanceof Admin)) throw new SecurityException("Admin access required");
    }
}
