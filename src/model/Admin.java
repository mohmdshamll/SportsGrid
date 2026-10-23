package model;

/** INHERITANCE: an Admin IS-A User. Moderates turfs, users and reviews. */
public class Admin extends User {

    private int adminLevel = 1;   // not stored in the database; kept to match the UML

    public Admin(int userId, String name, String email, String phone, String passwordHash) {
        super(userId, name, email, phone, passwordHash);
    }

    @Override
    public Role getRole() { return Role.ADMIN; }

    @Override
    public String getDashboard() {
        return "Admin dashboard for " + getName() + " (level " + adminLevel
                + "): approve turfs, manage users, remove reviews.";
    }

    public int getAdminLevel() { return adminLevel; }
    public void setAdminLevel(int adminLevel) { this.adminLevel = adminLevel; }
}
