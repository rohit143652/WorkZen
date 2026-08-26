package com.example.application.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success = false;
    private String message;
    private int status;
    private String path;
    private Instant timestamp = Instant.now();

    public ErrorResponse() {}

    public ErrorResponse(String message, int status, String path) {
        this.message = message;
        this.status = status;
        this.path = path;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Instant getTimestamp() { return timestamp; }
}
