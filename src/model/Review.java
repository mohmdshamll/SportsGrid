package model;

import java.time.LocalDateTime;

/** A 1-5 star rating and comment from a Player about a Turf. */
public class Review {

    private int reviewId;
    private int playerId;
    private int turfId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String playerName;   // filled when loading, for display only

    public Review(int reviewId, int playerId, int turfId, int rating, String comment) {
        this.reviewId = reviewId;
        this.playerId = playerId;
        this.turfId = turfId;
        this.rating = rating;
        this.comment = comment;
    }

    public boolean validateRating() { return rating >= 1 && rating <= 5; }

    public int getReviewId() { return reviewId; }
    public void setReviewId(int reviewId) { this.reviewId = reviewId; }
    public int getPlayerId() { return playerId; }
    public int getTurfId() { return turfId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    @Override
    public String toString() { return rating + "/5 - " + comment + (playerName != null ? " (" + playerName + ")" : ""); }
}
