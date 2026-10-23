package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** One bookable window (date + start/end time) of a turf. */
public class TimeSlot {

    private int slotId;
    private int turfId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean booked;

    public TimeSlot(int slotId, int turfId, LocalDate date, LocalTime startTime, LocalTime endTime, boolean booked) {
        this.slotId = slotId;
        this.turfId = turfId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.booked = booked;
    }

    /** ENCAPSULATION: callers change 'booked' only through these rule-checking methods. */
    public void book() {
        if (booked) throw new IllegalStateException("Slot is already booked");
        booked = true;
    }

    public void release() { booked = false; }

    public boolean isAvailable() { return !booked; }

    /** Length of the slot in hours (e.g. 18:00-19:30 = 1.5). */
    public double getDurationHours() {
        return java.time.Duration.between(startTime, endTime).toMinutes() / 60.0;
    }

    public boolean isInPast() {
        return LocalDateTime.of(date, startTime).isBefore(LocalDateTime.now());
    }

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }
    public int getTurfId() { return turfId; }
    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public boolean isBooked() { return booked; }

    @Override
    public String toString() { return date + " " + startTime + "-" + endTime + (booked ? " (booked)" : ""); }
}
