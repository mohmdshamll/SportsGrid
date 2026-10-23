package model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** A turf listed by an owner. A Turf is COMPOSED of its TimeSlots. */
public class Turf {

    private int turfId;
    private int ownerId;
    private String ownerName;                        // loaded from users table via JOIN
    private String name;
    private String location;
    private String sportType;
    private double pricePerHour;
    private boolean approved;
    private double averageRating;                    // filled by TurfDAO from reviews
    private List<TimeSlot> slots = new ArrayList<>();

    public Turf(int turfId, int ownerId, String name, String location, String sportType,
                double pricePerHour, boolean approved) {
        this.turfId = turfId;
        this.ownerId = ownerId;
        this.name = name;
        this.location = location;
        this.sportType = sportType;
        this.pricePerHour = pricePerHour;
        this.approved = approved;
    }

    public void addSlot(TimeSlot slot) { slots.add(slot); }

    public List<TimeSlot> getAvailableSlots() {
        return slots.stream().filter(TimeSlot::isAvailable).collect(Collectors.toList());
    }

    public int getTurfId() { return turfId; }
    public void setTurfId(int turfId) { this.turfId = turfId; }
    public int getOwnerId() { return ownerId; }
    public String getOwnerName() {
        return (ownerName != null && !ownerName.isBlank()) ? ownerName : ("Owner #" + ownerId);
    }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getSportType() { return sportType; }
    public void setSportType(String sportType) { this.sportType = sportType; }
    public double getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(double pricePerHour) { this.pricePerHour = pricePerHour; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public List<TimeSlot> getSlots() { return slots; }

    @Override
    public String toString() { return name + " - " + sportType + " - Rs " + pricePerHour + "/hr (" + location + ")"; }
}
