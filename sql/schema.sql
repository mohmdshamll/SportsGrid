-- SportGrid database schema (MySQL 8+)
-- Run once:  mysql -u root -p < sql/schema.sql   (or paste into MySQL Workbench and run)

CREATE DATABASE IF NOT EXISTS sportgrid CHARACTER SET utf8mb4;
USE sportgrid;

CREATE TABLE IF NOT EXISTS users (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(120) NOT NULL UNIQUE,
    phone          VARCHAR(15),
    password_hash  VARCHAR(200) NOT NULL,          -- never store plain passwords
    role           ENUM('PLAYER','OWNER','ADMIN') NOT NULL DEFAULT 'PLAYER',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS turfs (
    turf_id         INT AUTO_INCREMENT PRIMARY KEY,
    owner_id        INT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    location        VARCHAR(150) NOT NULL,
    sport_type      VARCHAR(40)  NOT NULL,
    price_per_hour  DECIMAL(8,2) NOT NULL CHECK (price_per_hour > 0),
    is_approved     BOOLEAN NOT NULL DEFAULT FALSE,  -- admin must approve before players see it
    FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS time_slots (
    slot_id      INT AUTO_INCREMENT PRIMARY KEY,
    turf_id      INT NOT NULL,
    slot_date    DATE NOT NULL,
    start_time   TIME NOT NULL,
    end_time     TIME NOT NULL,
    is_booked    BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uq_slot (turf_id, slot_date, start_time),
    FOREIGN KEY (turf_id) REFERENCES turfs(turf_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bookings (
    booking_id    INT AUTO_INCREMENT PRIMARY KEY,
    player_id     INT NOT NULL,
    turf_id       INT NOT NULL,
    slot_id       INT NOT NULL,
    total_amount  DECIMAL(10,2) NOT NULL,
    status        ENUM('PENDING','CONFIRMED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    booking_date  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_id) REFERENCES users(user_id)      ON DELETE CASCADE,
    FOREIGN KEY (turf_id)   REFERENCES turfs(turf_id)      ON DELETE CASCADE,
    FOREIGN KEY (slot_id)   REFERENCES time_slots(slot_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payments (
    payment_id   INT AUTO_INCREMENT PRIMARY KEY,
    booking_id   INT NOT NULL,
    method       ENUM('UPI','CARD') NOT NULL,
    reference    VARCHAR(60),                       -- UPI id, or last 4 digits of card only
    amount       DECIMAL(10,2) NOT NULL,
    status       ENUM('PENDING','SUCCESS','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reviews (
    review_id    INT AUTO_INCREMENT PRIMARY KEY,
    player_id    INT NOT NULL,
    turf_id      INT NOT NULL,
    rating       INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      VARCHAR(500),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_review (player_id, turf_id),      -- one review per player per turf
    FOREIGN KEY (player_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (turf_id)   REFERENCES turfs(turf_id) ON DELETE CASCADE
);
