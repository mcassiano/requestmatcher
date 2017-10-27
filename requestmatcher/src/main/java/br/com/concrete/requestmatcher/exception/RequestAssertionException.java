package br.com.concrete.requestmatcher.exception;

public final class RequestAssertionException extends RuntimeException {

    public RequestAssertionException(String message, Throwable e) {
        super(message + " " + e.getMessage(), e);
    }
}
