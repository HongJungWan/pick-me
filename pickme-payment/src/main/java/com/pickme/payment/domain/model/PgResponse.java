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
}
