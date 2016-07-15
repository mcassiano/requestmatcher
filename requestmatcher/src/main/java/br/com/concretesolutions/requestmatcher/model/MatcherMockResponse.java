package br.com.concretesolutions.requestmatcher.model;

import br.com.concretesolutions.requestmatcher.RequestMatcher;
import okhttp3.mockwebserver.MockResponse;

public final class MatcherMockResponse {

    private final MockResponse response;
    private final RequestMatcher matcher;

    public MatcherMockResponse(RequestMatcher matcher, MockResponse response) {
        this.matcher = matcher;
        this.response = response;
    }

    public MockResponse getResponse() {
        return response;
    }

    public RequestMatcher getMatcher() {
        return matcher;
    }
}
