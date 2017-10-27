package br.com.concrete.requestmatcher.exception;

import br.com.concrete.requestmatcher.RequestMatchersGroup;
import okhttp3.Headers;
import okhttp3.mockwebserver.RecordedRequest;

public class NoMatchersForRequestException extends RuntimeException {

    private static final String PRE_WITH_REQ = "No matcher found for request: \n\n";

    private NoMatchersForRequestException(Builder builder) {
        super(builder.sb.toString());
    }

    public static class Builder {

        @SuppressWarnings("PMD.AvoidStringBufferField")
        private final StringBuilder sb = new StringBuilder(PRE_WITH_REQ);

        public Builder(final RecordedRequest request) {
            buildRequestMessage(sb, request)
                    .append("\n\nTried the following matchers:\n");
        }

        public Builder appendAssertionError(final int order,
                                            final AssertionError error,
                                            final RequestMatchersGroup matcher) {

            sb.append('\n').append(order).append(". ");

            final String message = error
                    .toString()
                    .substring(error.getClass().getCanonicalName().length() + 1);

            matcher.buildExpectedMatchers(sb)
                    .append("\n Failed because")
                    .append(message.replaceAll("\n", "\n "))
                    .append('\n');
            return this;
        }

        public NoMatchersForRequestException build() {
            return new NoMatchersForRequestException(this);
        }
    }

    private static StringBuilder buildRequestMessage(final StringBuilder sb,
                                                     final RecordedRequest request) {

        sb.append("> ").append(request.getRequestLine());

        final Headers headers = request.getHeaders();
        for (int i = 0, count = headers.size(); i < count; i++) {
            sb.append("\n> ").append(headers.name(i)).append(": ").append(headers.value(i));
        }

        final String body = request.getBody().clone().readUtf8();
        return body.isEmpty() ? sb : sb.append("\n\n").append(body);
    }
}
