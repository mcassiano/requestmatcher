package br.com.concretesolutions.requestmatcher;

public final class RequestAssertionException extends RuntimeException {

    public RequestAssertionException(String message, Error e) {
        super(message + " " + e.getMessage(), e);
    }
}
