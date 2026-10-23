package service;

import dao.BookingDAO;
import dao.ReviewDAO;
import dao.TimeSlotDAO;
import dao.TurfDAO;
import db.DuplicateEntryException;
import model.Booking;
import model.Player;
import model.Review;
import model.TimeSlot;
import model.Turf;
import model.TurfOwner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/** Everything a turf owner does (turfs, slots, earnings) plus reviews. */
public class TurfService {

    private final TurfDAO turfDAO = new TurfDAO();
    private final TimeSlotDAO slotDAO = new TimeSlotDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();

    // ---------------- turfs ----------------

    /** New turfs start unapproved; an Admin must approve them before players can see them. */
    public Turf addTurf(TurfOwner owner, String name, String location, String sportType, double pricePerHour) {
        validateTurfFields(name, location, sportType, pricePerHour);
        Turf turf = new Turf(0, owner.getUserId(), name.trim(), location.trim(), sportType.trim(), pricePerHour, false);
        turfDAO.insert(turf);
        return turf;
    }

    public void updateTurf(TurfOwner owner, int turfId, String name, String location, String sportType, double pricePerHour) {
        validateTurfFields(name, location, sportType, pricePerHour);
        Turf turf = requireOwnedTurf(owner, turfId);
        turf.setName(name.trim());
        turf.setLocation(location.trim());
        turf.setSportType(sportType.trim());
        turf.setPricePerHour(pricePerHour);
        turfDAO.update(turf);
    }

    /** Loads this owner's turfs and earnings into the TurfOwner object (used by getDashboard()). */
    public List<Turf> loadOwnerDashboard(TurfOwner owner) {
        List<Turf> turfs = turfDAO.findByOwner(owner.getUserId());
        owner.setOwnedTurfs(turfs);
        owner.setTotalEarnings(bookingDAO.sumConfirmedForOwner(owner.getUserId()));
        return turfs;
    }

    public List<Booking> getOwnerBookings(TurfOwner owner) {
        return bookingDAO.findByOwner(owner.getUserId());
    }

    // ---------------- slots ----------------

    public TimeSlot addSlot(TurfOwner owner, int turfId, LocalDate date, LocalTime start, LocalTime end) {
        requireOwnedTurf(owner, turfId);
        if (date == null || start == null || end == null) throw new IllegalArgumentException("Date and times are required");
        if (!end.isAfter(start)) throw new IllegalArgumentException("End time must be after start time");
        if (LocalDateTime.of(date, start).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("You cannot add a slot in the past");
        }
        TimeSlot slot = new TimeSlot(0, turfId, date, start, end, false);
        try {
            slotDAO.insert(slot);
        } catch (DuplicateEntryException e) {
            throw new IllegalArgumentException("A slot starting at that time already exists");
        }
        return slot;
    }

    /**
     * Quickly creates one-hour slots from openHour up to closeHour for a date,
     * e.g. (6, 22) makes 06:00-07:00 ... 21:00-22:00. Skips slots that already exist or are in the past.
     * @return how many slots were created
     */
    public int generateHourlySlots(TurfOwner owner, int turfId, LocalDate date, int openHour, int closeHour) {
        requireOwnedTurf(owner, turfId);
        if (date == null) throw new IllegalArgumentException("Date is required");
        if (openHour < 0 || closeHour > 23 || openHour >= closeHour) {
            throw new IllegalArgumentException("Opening hour must be before closing hour (0-23)");
        }
        int created = 0;
        for (int h = openHour; h < closeHour; h++) {
            LocalTime start = LocalTime.of(h, 0);
            if (LocalDateTime.of(date, start).isBefore(LocalDateTime.now())) continue;
            try {
                slotDAO.insert(new TimeSlot(0, turfId, date, start, LocalTime.of(h + 1, 0), false));
                created++;
            } catch (DuplicateEntryException ignored) {
                // slot already exists, skip it
            }
        }
        return created;
    }

    /** All upcoming slots (free and booked) so the owner can see the full calendar. */
    public List<TimeSlot> getSlots(TurfOwner owner, int turfId) {
        requireOwnedTurf(owner, turfId);
        return slotDAO.findByTurf(turfId, false);
    }

    /** Only unbooked slots can be removed. */
    public boolean removeSlot(TurfOwner owner, int turfId, int slotId) {
        requireOwnedTurf(owner, turfId);
        return slotDAO.delete(slotId);
    }

    // ---------------- reviews ----------------

    /** A player may review a turf once, and only after they have a confirmed booking there. */
    public Review addReview(Player player, int turfId, int rating, String comment) {
        Review review = new Review(0, player.getUserId(), turfId, rating, comment == null ? "" : comment.trim());
        if (!review.validateRating()) throw new IllegalArgumentException("Rating must be between 1 and 5");
        if (review.getComment().length() > 500) throw new IllegalArgumentException("Comment is too long (max 500 characters)");
        if (!bookingDAO.hasConfirmedBooking(player.getUserId(), turfId)) {
            throw new IllegalStateException("You can review a turf only after booking it");
        }
        try {
            reviewDAO.insert(review);
        } catch (DuplicateEntryException e) {
            throw new IllegalStateException("You have already reviewed this turf");
        }
        return review;
    }

    public List<Review> getReviews(int turfId) {
        return reviewDAO.findByTurf(turfId);
    }

    public boolean hasReviewed(Player player, int turfId) {
        if (player == null) return false;
        return reviewDAO.hasPlayerReviewed(player.getUserId(), turfId);
    }

    public Optional<Review> getPlayerReview(Player player, int turfId) {
        if (player == null) return Optional.empty();
        return reviewDAO.findByTurfAndPlayer(turfId, player.getUserId());
    }

    // ---------------- helpers ----------------

    private Turf requireOwnedTurf(TurfOwner owner, int turfId) {
        return turfDAO.findById(turfId)
                .filter(t -> t.getOwnerId() == owner.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Turf not found"));
    }

    private void validateTurfFields(String name, String location, String sportType, double price) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Turf name is required");
        if (location == null || location.isBlank()) throw new IllegalArgumentException("Location is required");
        if (sportType == null || sportType.isBlank()) throw new IllegalArgumentException("Sport type is required");
        if (price <= 0) throw new IllegalArgumentException("Price per hour must be more than 0");
        if (name.trim().length() > 100 || location.trim().length() > 150 || sportType.trim().length() > 40) {
            throw new IllegalArgumentException("One of the fields is too long");
        }
    }
}
