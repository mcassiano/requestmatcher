package br.com.concretesolutions.requestmatcher;

import android.util.Pair;

import java.util.HashSet;
import java.util.Set;

import br.com.concretesolutions.requestmatcher.assertion.BodyAssertion;
import br.com.concretesolutions.requestmatcher.assertion.RequestAssertion;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.fail;

public final class RequestMatcher {

    public static final String GET = "GET", POST = "POST", DELETE = "DELETE", PUT = "PUT";

    private final Set<Pair<String, String>> expectedQueries = new HashSet<>();
    private RequestAssertion requestAssert;
    private BodyAssertion bodyAssertion;
    private String expectedPath;
    private String expectedMethod;
    private boolean expectNoBody;

    public RequestMatcher assertRequest(RequestAssertion requestAssert) {
        this.requestAssert = requestAssert;
        return this;
    }

    public RequestMatcher assertPathIs(String expectedPath) {
        this.expectedPath = expectedPath;
        return this;
    }

    public RequestMatcher assertBody(BodyAssertion bodyAssertion) {

        if (expectNoBody)
            throw new IllegalArgumentException("Cannot assertBody and assertNoBody together");

        this.bodyAssertion = bodyAssertion;
        return this;
    }

    public RequestMatcher assertNoBody() {

        if (bodyAssertion != null)
            throw new IllegalArgumentException("Cannot assertBody and assertNoBody together");

        this.expectNoBody = true;
        return this;
    }

    public RequestMatcher assertHasQuery(String key, String value) {
        expectedQueries.add(Pair.create(key, value));
        return this;
    }

    public RequestMatcher assertMethodIs(String method) {
        this.expectedMethod = method;
        return this;
    }


    public void doAssert(RecordedRequest request) {

        if (requestAssert != null)
            requestAssert.doAssert(request);

        if (expectedMethod != null)
            assertThat(request.getMethod(), is(expectedMethod));

        final String path = request.getPath();

        if (expectedPath != null)
            assertThat(path, is(expectedPath));

        queryAssertions(path);
        bodyAssertions(request);
    }

    private void queryAssertions(String path) {

        if (expectedQueries.isEmpty())
            return;

        if (!RequestUtils.hasQuery(path))
            fail("Expected query strings but found none");


        final Set<Pair<String, String>> allQueries = RequestUtils.buildQueries(path);
        for (Pair<String, String> query : expectedQueries)
            assertThat(query, isIn(allQueries));
    }

    private void bodyAssertions(RecordedRequest request) {
        final String body = RequestUtils.getBody(request);

        if (expectNoBody)
            assertThat(body, isEmptyString());

        if (bodyAssertion != null) {

            if ("".equals(body))
                fail("Expected body but found none");

            bodyAssertion.doAssert(body);
        }
    }

    // Ensure we call Object.hashCode for instance detection
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
