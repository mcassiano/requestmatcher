package br.com.concretesolutions.requestmatcher;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

import static org.junit.Assert.fail;

public abstract class RequestMatcherRule implements TestRule {

    private static final String ASSERT_HEADER = "REQUEST-ASSERT", ERROR_MESSAGE = "Failed assertion for %s";

    private final Map<String, RequestMatcher> requestAssertions = new HashMap<>();
    private final MockWebServer server;

    private RequestAssertionError assertionError;

    public RequestMatcherRule() {
        this(new MockWebServer());
    }

    public RequestMatcherRule(MockWebServer server) {
        this.server = server;
    }

    protected abstract InputStream open(String path) throws IOException;

    @Override
    public Statement apply(Statement base, Description description) {
        return requestAssertionStatement(base);
    }

    public RequestMatcher enqueueDisconnect() {
        return enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
    }

    public RequestMatcher enqueue(MockResponse response) {
        final RequestMatcher requestMatcher = new RequestMatcher();
        final String assertPath = response.hashCode() + "_::_" + requestMatcher.hashCode();
        server.enqueue(response.setHeader(ASSERT_HEADER, assertPath));
        // Only enqueue request if eveything else passed. An exception thrown here would
        // make the request count be different.
        requestAssertions.put(assertPath, requestMatcher);
        return requestMatcher;
    }

    /**
     * Helper method to enqueueNoBody a mock response without body.
     *
     * @param statusCode status code of response
     */
    public RequestMatcher enqueueNoBody(int statusCode) {
        return enqueue(new MockResponse()
                .setResponseCode(statusCode)
                .setBody(""));
    }

    /**
     * Helper method to enqueue a mock response.
     * Uses {@link IOReader#read(InputStream)} (String)} to read json from assetPath.
     *
     * @param statusCode status code of response
     * @param assetPath  Path inside the "json" folder in androidTest/assets
     */
    public RequestMatcher enqueue(int statusCode, String assetPath) {
        return enqueue(new MockResponse()
                .setResponseCode(statusCode)
                .setBody(readFixture(assetPath)));
    }

    public RequestMatcher enqueueGET(int statusCode) {
        return enqueueNoBody(statusCode).assertNoBody().assertMethodIs(RequestMatcher.GET);
    }

    public RequestMatcher enqueueGET(int statusCode, String assetPath) {
        return enqueue(statusCode, assetPath).assertNoBody().assertMethodIs(RequestMatcher.GET);
    }

    public RequestMatcher enqueuePOST(int statusCode) {
        return enqueueNoBody(statusCode).assertMethodIs(RequestMatcher.POST);
    }

    public RequestMatcher enqueuePOST(int statusCode, String assetPath) {
        return enqueue(statusCode, assetPath).assertMethodIs(RequestMatcher.POST);
    }

    public RequestMatcher enqueuePUT(int statusCode) {
        return enqueueNoBody(statusCode).assertMethodIs(RequestMatcher.PUT);
    }

    public RequestMatcher enqueuePUT(int statusCode, String assetPath) {
        return enqueue(statusCode, assetPath).assertMethodIs(RequestMatcher.PUT);
    }

    public HttpUrl url(String path) {
        return server.url(path);
    }

    public MockWebServer getMockWebServer() {
        return server;
    }

    private void before() {
        this.server.setDispatcher(new QueueDispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

                final MockResponse response = super.dispatch(request);

                final RequestMatcher matcher = requestAssertions.get(response.getHeaders().get("REQUEST-ASSERT"));

                if (matcher != null)
                    try {
                        matcher.doAssert(request);
                    } catch (AssertionError e) {
                        final String message = String.format(ERROR_MESSAGE, request);
                        RequestMatcherRule.this.assertionError = new RequestAssertionError(message, e);
                        return new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_END);
                    }

                return response;
            }
        });
    }

    private void after() {
        final int requestQueueSize = requestAssertions.size();
        final int requestCount = server.getRequestCount();

        try {
            if (assertionError != null)
                throw assertionError;

            if (requestQueueSize != requestCount)
                fail("Failed assertion. Enqueued " + requestQueueSize + " requests but used " + requestCount + " requests.");

        } finally {
            assertionError = null;
            requestAssertions.clear();
        }
    }

    private Statement requestAssertionStatement(final Statement base) {

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }

    private String readFixture(final String assetPath) {
        try {
            return IOReader.read(open("fixtures/" + assetPath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read asset with path " + assetPath, e);
        }
    }
}
