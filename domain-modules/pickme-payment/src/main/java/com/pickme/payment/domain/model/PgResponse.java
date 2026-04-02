package com.pickme.payment.domain.model;

public class PgResponse {

    private final String transactionId;
    private final boolean success;
    private final String message;

    public PgResponse(String transactionId, boolean success, String message) {
        this.transactionId = transactionId;
        this.success = success;
        this.message = message;
    }

    public static PgResponse success(String transactionId) {
        return new PgResponse(transactionId, true, "승인 완료");
    }

    public static PgResponse failure(String message) {
        return new PgResponse(null, false, message);
    }

    public String getTransactionId() { return transactionId; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PgResponse that = (PgResponse) o;
        return success == that.success && java.util.Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(transactionId, success);
    }
}
