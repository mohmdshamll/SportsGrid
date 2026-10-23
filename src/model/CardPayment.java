package model;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Pays with a card. The full card number is checked once (length + Luhn checksum) and then
 * thrown away - only the last 4 digits are kept, so it can never leak from the database.
 */
public class CardPayment extends Payment {

    private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("MM/yy");

    private String cardHolder;
    private String last4Digits;
    private String expiry;
    private boolean numberValid;

    public CardPayment(String cardHolder, String cardNumber, String expiry) {
        super();
        String digits = cardNumber == null ? "" : cardNumber.replaceAll("[\\s-]", "");
        this.cardHolder = cardHolder == null ? "" : cardHolder.trim();
        this.expiry = expiry == null ? "" : expiry.trim();
        this.numberValid = digits.matches("\\d{13,19}") && passesLuhn(digits);
        this.last4Digits = digits.length() >= 4 ? digits.substring(digits.length() - 4) : "";
    }

    @Override
    public boolean processPayment() {
        if (!cardHolder.isEmpty() && numberValid && !isExpired()) {
            markSuccess();
            return true;
        }
        markFailed();
        return false;
    }

    private boolean isExpired() {
        try {
            YearMonth exp = YearMonth.parse(expiry, EXPIRY_FORMAT);
            return exp.isBefore(YearMonth.now());
        } catch (DateTimeParseException e) {
            return true;   // unreadable expiry counts as invalid
        }
    }

    /** Standard card checksum (Luhn algorithm). */
    private static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean alt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (alt) { d *= 2; if (d > 9) d -= 9; }
            sum += d;
            alt = !alt;
        }
        return sum % 10 == 0;
    }

    @Override public String getMethod() { return "CARD"; }
    @Override public String getReference() { return last4Digits; }
    public String getCardHolder() { return cardHolder; }
    public String getLast4Digits() { return last4Digits; }
}
