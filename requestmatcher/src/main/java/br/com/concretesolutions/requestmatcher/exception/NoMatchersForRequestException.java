package br.com.concretesolutions.requestmatcher.exception;

import java.util.Set;

import br.com.concretesolutions.requestmatcher.MatcherDispatcher;
import okhttp3.mockwebserver.RecordedRequest;

public class NoMatchersForRequestException extends RuntimeException {

    private static final String PRE = "No matchers found for request. Failing test. Current matchers: ";
    private static final String PRE_WITH_REQ = "\nNo matcher found for request: ";

    public NoMatchersForRequestException(
            RecordedRequest request,
            StringBuffer notMatchedAsserts,
            Set<MatcherDispatcher.ResponseWithMatcher> matchers) {
        super(buildMessage(request, notMatchedAsserts, matchers));
    }

    public NoMatchersForRequestException(Set<MatcherDispatcher.ResponseWithMatcher> matchers) {
        super(buildMessage(matchers));
    }

    private static String buildMessage(RecordedRequest request, StringBuffer notMatchedAsserts, Set<MatcherDispatcher.ResponseWithMatcher> matchers) {
        final StringBuilder sb = new StringBuilder(PRE_WITH_REQ);
        sb.append(request.toString()).append('\n');
        sb.append("with body: '").append(request.getBody().toString()).append("'\n");
        sb.append("Listing all failed assertion messages (we've tried each of your matchers!):").append('\n');
        sb.append(notMatchedAsserts);
        sb.append(buildMessage(matchers));
        return sb.toString();
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
