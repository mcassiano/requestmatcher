package br.com.concretesolutions.requestmatcher;

public final class RequestAssertionError extends RuntimeException {

    public RequestAssertionError(String message, Error e) {
        super(message + " " + e.getMessage(), e);
    }
}
