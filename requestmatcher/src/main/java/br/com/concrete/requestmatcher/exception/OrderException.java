package br.com.concrete.requestmatcher.exception;

import java.util.Locale;

public final class OrderException extends RuntimeException {

    private static final String MSG = "Order of request is different than expected. "
            + "Expected orderIs: %d but was: %d";

    public OrderException(int expected, int received) {
        super(String.format(Locale.ENGLISH, MSG, expected, received));
    }
}
