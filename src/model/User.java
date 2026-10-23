package model;

/**
 * ABSTRACTION + ENCAPSULATION: base class for every kind of user.
 * It cannot be instantiated directly (abstract), and all fields are private.
 * Subclasses must say what their role is and what their dashboard shows (POLYMORPHISM).
 */
public abstract class User {

    private int userId;
    private String name;
    private String email;
    private String phone;
    private String passwordHash;   // never the plain password

    protected User(int userId, String name, String email, String phone, String passwordHash) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
    }

    /** Factory: builds the right subclass for a role, so DAOs do not need a switch everywhere. */
    public static User create(Role role, int id, String name, String email, String phone, String hash) {
        switch (role) {
            case PLAYER: return new Player(id, name, email, phone, hash);
            case OWNER:  return new TurfOwner(id, name, email, phone, hash);
            case ADMIN:  return new Admin(id, name, email, phone, hash);
            default: throw new IllegalArgumentException("Unknown role: " + role);
        }
    }

    public abstract Role getRole();

    /** POLYMORPHISM: every subclass overrides this with its own summary text. */
    public abstract String getDashboard();

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    @Override
    public String toString() { return name + " (" + getRole() + ")"; }
}
