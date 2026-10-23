package model;

import java.util.ArrayList;
import java.util.List;

/** INHERITANCE: a TurfOwner IS-A User. Lists turfs and earns money from bookings. */
public class TurfOwner extends User {

    private List<Turf> ownedTurfs = new ArrayList<>();
    private double totalEarnings;

    public TurfOwner(int userId, String name, String email, String phone, String passwordHash) {
        super(userId, name, email, phone, passwordHash);
    }

    @Override
    public Role getRole() { return Role.OWNER; }

    @Override
    public String getDashboard() {
        return "Owner dashboard for " + getName() + ": " + ownedTurfs.size()
                + " turfs, total earnings Rs " + String.format("%.2f", totalEarnings) + ".";
    }

    public List<Turf> getOwnedTurfs() { return ownedTurfs; }
    public void setOwnedTurfs(List<Turf> ownedTurfs) { this.ownedTurfs = ownedTurfs; }

    public double getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(double totalEarnings) { this.totalEarnings = totalEarnings; }
}
