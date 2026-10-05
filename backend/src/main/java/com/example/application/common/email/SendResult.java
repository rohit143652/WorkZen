package com.example.application.common.email;

/** Outcome of one send attempt. {@code failureReason} is a short plain-language cause (safe to show a Super Admin), null on success. */
public record SendResult(boolean sent, String failureReason) {
    public static SendResult ok() { return new SendResult(true, null); }
    public static SendResult failed(String reason) { return new SendResult(false, reason); }
}
