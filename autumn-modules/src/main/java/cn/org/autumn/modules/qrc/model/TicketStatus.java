package cn.org.autumn.modules.qrc.model;

public final class TicketStatus {
    public static final String PENDING = "PENDING";
    public static final String SCANNED = "SCANNED";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String COMPLETED = "COMPLETED";
    public static final String EXPIRED = "EXPIRED";
    public static final String CANCELLED = "CANCELLED";
    public static final String DENIED = "DENIED";

    private TicketStatus() {
    }

    public static boolean isTerminal(String status) {
        return COMPLETED.equals(status) || EXPIRED.equals(status) || CANCELLED.equals(status) || DENIED.equals(status);
    }
}
