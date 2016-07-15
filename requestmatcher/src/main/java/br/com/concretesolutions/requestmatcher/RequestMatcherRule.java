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
 * A wrapping rule around {@link MockWebServer} to ease mocking. This provides: <ul> <li>Fixtures
 * setup: you can have a folder named fixtures in your resources and this rule will load them for
 * you and put them in your response's body</li> <li>Status code setup: you can pass a response's
 * status code when enqueuing</li> <li>Request assertions: when enqueuing you can add assertions to
 * ensure that the request arrived is the one expected.</li> </ul>
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

    public HttpUrl url(String path) {
        return server.url(path);
    }

    public MockWebServer getMockWebServer() {
        return server;
    }

    public String readFixture(final String assetPath) {
        try {
            return IOReader.read(open(fixturesRootFolder + "/" + assetPath)) + "\n";
        } catch (IOException e) {
            throw new RuntimeException("Failed to read asset with path " + assetPath, e);
        }
    }

    public RequestMatcher enqueue(MockResponse response) {
        return dispatcher.enqueue(response);
    }

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

    private void after(boolean success) {

        if (dispatcher.getAssertionError() != null)
            throw dispatcher.getAssertionError();

        if (!success)
            return;

        if (dispatcher.getQueue().size() != 0) {
            try {
                fail("There are enqueued requests that were not used.");
            } catch (AssertionError e) {
                throw new RequestAssertionError("Failed assertion.", e);
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

                } finally {
                    after(success);
                }
            }
        };
    }
}
