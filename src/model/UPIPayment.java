package model;

import java.util.regex.Pattern;

/** Pays with a UPI id such as name@okhdfc. Overrides processPayment() (POLYMORPHISM). */
public class UPIPayment extends Payment {

    private static final Pattern UPI_FORMAT = Pattern.compile("^[A-Za-z0-9._\\-]{2,}@[A-Za-z]{2,}$");

    private String upiId;

    public UPIPayment(String upiId) {
        super();
        this.upiId = upiId == null ? "" : upiId.trim();
    }

    @Override
    public boolean processPayment() {
        if (UPI_FORMAT.matcher(upiId).matches()) {
            markSuccess();
            return true;
        }
        markFailed();
        return false;
    }

    @Override public String getMethod() { return "UPI"; }
    @Override public String getReference() { return upiId; }
    public String getUpiId() { return upiId; }
}
