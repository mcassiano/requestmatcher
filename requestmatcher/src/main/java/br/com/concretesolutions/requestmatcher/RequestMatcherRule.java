package br.com.concretesolutions.requestmatcher;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.fail;

/**
 * A wrapping rule around {@link MockWebServer} to ease mocking. This provides:
 * <ul>
 * <li>Fixtures
 * setup: you can have a folder named fixtures in your resources and this rule will load them for
 * you and put them in your response's body</li>
 * <li>Status code setup: you can pass a response's status code when enqueuing</li>
 * <li>Request assertions: when enqueuing you can add assertions to
 * ensure that the request arrived is the one expected.</li>
 * </ul>
 * <p>
 * This rule provides helper methods for enqueuing responses with the corresponding request's
 * assertions. When using one of the enqueue methods here the return is a {@link RequestMatcher}
 * instance for easy method chaining.
 *
 * @see TestRule
 * @see MockWebServer
 * @see RequestMatcher
 */
public abstract class RequestMatcherRule implements TestRule {

    private final MatcherQueueDispatcher dispatcher = new MatcherQueueDispatcher();
    private final MockWebServer server;
    private final String fixturesRootFolder;

    /**
     * Creates a rule with a new instance of {@link MockWebServer} and no logging of request lines.
     * This will by default look for fixtures in the "fixtures" folder.
     */
    public RequestMatcherRule() {
        this(new MockWebServer());
    }

    /**
     * Creates a rule with a new instance of {@link MockWebServer} and no logging of request lines.
     *
     * @param fixturesRootFolder The root folder to look for fixtures. Defaults to "fixtures"
     */
    public RequestMatcherRule(String fixturesRootFolder) {
        this(new MockWebServer(), fixturesRootFolder);
    }

    /**
     * Creates a rule with the given instance of {@link MockWebServer} and no logging of request
     * lines. This will by default look for fixtures in the "fixtures" folder.
     *
     * @param server The {@link MockWebServer} instance
     */
    public RequestMatcherRule(MockWebServer server) {
        this(server, "fixtures");
    }

    public RequestMatcherRule(MockWebServer server, String fixturesRootFolder) {
        this.server = server;
        this.fixturesRootFolder = fixturesRootFolder;
    }

    // implemented by Unit and Instrumented dispatchers to find fixtures folder
    protected abstract InputStream open(String path) throws IOException;

    @Override
    public Statement apply(Statement base, Description description) {
        return server.apply(requestAssertionStatement(base), description);
    }

    /**
     * Returns the complete url of the relative path.
     *
     * @param path A relative path. For example: "/" would return http://localhost:<portnumber></portnumber>/
     * @return An OkHttp URL
     */
    public HttpUrl url(String path) {
        return server.url(path);
    }

    public MockWebServer getMockWebServer() {
        return server;
    }

    /**
     * Used to read fixtures. This combines the fixturesRootFolder with the passed fixturePath to
     * find the file to read.
     *
     * @param fixturePath Relative path
     * @return The contents of the fixture
     */
    public String readFixture(final String fixturePath) {
        try {
            return IOReader.read(open(fixturesRootFolder + "/" + fixturePath)) + "\n";
        } catch (IOException e) {
            throw new RuntimeException("Failed to read asset with path " + fixturePath, e);
        }
    }

    /**
     * Enqueues a MockResponse in the dispatcher. This creates a default RequestMatcher that can be
     * then configured to assert on the received request.
     *
     * @param response The mocked response to enqueue in order.
     * @return RequestMatcher instance for method chaining
     */
    public RequestMatcher enqueue(MockResponse response) {
        return dispatcher.enqueue(response);
    }

    /**
     * Same as {@link #enqueue(MockResponse)} but accepts an instance of a class that extends
     * {@link RequestMatcher} that may contain custom assertions.
     *
     * @param response The mocked response to enqueue in order.
     * @param matcher  A custom {@link RequestMatcher}
     * @return RequestMatcher instance for method chaining. This is the same instance passed as an
     * argument
     */
    public <T extends RequestMatcher> T enqueue(MockResponse response, T matcher) {
        return dispatcher.enqueue(response, matcher);
    }

    /**
     * Helper method for enqueuing a no body response. No body means an empty body.
     *
     * @param statusCode The status code of the response.
     * @return RequestMatcher instance for method chaining
     */
    public RequestMatcher enqueueNoBody(int statusCode) {
        return enqueue(new MockResponse()
                .setResponseCode(statusCode)
                .setBody(""));
    }

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

    public RequestMatcher enqueueDELETE(int statusCode) {
        return enqueueNoBody(statusCode).assertMethodIs(RequestMatcher.PUT);
    }

    public RequestMatcher enqueueDELETE(int statusCode, String assetPath) {
        return enqueue(statusCode, assetPath).assertMethodIs(RequestMatcher.PUT);
    }

    private void after(Exception exception, boolean success) throws Exception {

        if (dispatcher.getAssertionException() != null) {

            // if there was an exception in the test (for example a NPE) we print it before throwing
            // the request assertion. This might help debug where the test is failing.
            // We can't simply add it as suppressed as it was added only in API 19.
            if (exception != null)
                exception.printStackTrace();

            throw dispatcher.getAssertionException();
        }

        if (!success) {

            if (exception != null)
                throw exception;

            return;
        }

        if (dispatcher.getQueue().size() != 0) {
            try {
                fail("There are enqueued requests that were not used.");
            } catch (AssertionError e) {
                throw new RequestAssertionException("Failed assertion.", e);
            }
        }
    }

    private Statement requestAssertionStatement(final Statement base) {

        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                server.setDispatcher(dispatcher);
                boolean success = false;
                try {
                    base.evaluate();
                    success = true;

                } catch (Exception e) {
                    after(e, success);
                } finally {
                    after(null, success);
                }
            }
        };
    }
}
