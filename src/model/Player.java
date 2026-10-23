package model;

import java.util.ArrayList;
import java.util.List;

/** INHERITANCE: a Player IS-A User. Books turfs and writes reviews. */
public class Player extends User {

    private List<Booking> bookingHistory = new ArrayList<>();

    public Player(int userId, String name, String email, String phone, String passwordHash) {
        super(userId, name, email, phone, passwordHash);
    }

    @Override
    public Role getRole() { return Role.PLAYER; }

    @Override
    public String getDashboard() {
        long confirmed = bookingHistory.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        return "Player dashboard for " + getName() + ": " + bookingHistory.size()
                + " bookings (" + confirmed + " confirmed).";
    }

    public List<Booking> getBookingHistory() { return bookingHistory; }
    public void setBookingHistory(List<Booking> bookingHistory) { this.bookingHistory = bookingHistory; }
}
