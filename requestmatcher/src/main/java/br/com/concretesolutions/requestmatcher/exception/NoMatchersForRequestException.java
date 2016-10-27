package br.com.concretesolutions.requestmatcher.exception;

import java.util.Set;

import br.com.concretesolutions.requestmatcher.MatcherDispatcher;

public class NoMatchersForRequestException extends RuntimeException {

    private static final String PRE = "No matchers found for request. Failing test. Current matchers: ";

    public NoMatchersForRequestException(Set<MatcherDispatcher.ResponseWithMatcher> matchers) {
        super(buildMessage(matchers));
    }

    private static String buildMessage(Set<MatcherDispatcher.ResponseWithMatcher> matchers) {

        final StringBuilder sb = new StringBuilder(PRE);

        int order = 0;

        for (MatcherDispatcher.ResponseWithMatcher matcherResponse : matchers) {
            sb.append('\n')
                    .append(++order)
                    .append(": ")
                    .append(matcherResponse.getMatcher());
        }

        return sb.toString();
    }
}
