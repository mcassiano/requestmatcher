package br.com.concrete.requestmatcher;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.concrete.requestmatcher.exception.RequestAssertionException;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.junit.Assert.fail;

/**
 * A wrapping rule around {@link MockWebServer} to ease mocking. This provides:
 * <p>
 * <ul>
 * <p>
 * <li>Fixtures setup: you can have a folder named fixtures in your resources and this rule will
 * load them for you and put them in your response's body</li>
 * <p>
 * <li>Status code setup: you can pass a response's status code when enqueuing</li>
 * <p>
 * <li>Request assertions: when enqueuing you can add assertions to ensure that the request arrived
 * is the one expected.</li>
 * <p>
 * </ul> <p>
 * <p>
 * This rule provides helper methods for enqueuing responses with the corresponding request's
 * assertions. When using one of the mockResponse methods here the return is a {@link
 * RequestMatchersGroup} instance for easy method chaining.
 *
 * @see TestRule
 * @see MockWebServer
 * @see RequestMatchersGroup
 */
public abstract class RequestMatcherRule implements TestRule {

    private final MatcherDispatcher dispatcher = new MatcherDispatcher();
    private final MockWebServer server;
    private final String fixturesRootFolder;

    private final Map<String, String> defaultHeaders = new HashMap<>();
    private boolean guessMimeType = true;

    RequestMatcherRule() {
        this(new MockWebServer());
    }

    RequestMatcherRule(String fixturesRootFolder) {
        this(new MockWebServer(), fixturesRootFolder);
    }

    RequestMatcherRule(MockWebServer server) {
        this(server, "fixtures");
    }

    RequestMatcherRule(MockWebServer server, String fixturesRootFolder) {
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
     * Adds this header to all fixtures delivered by this rule.
     *
     * @param key   Header key
     * @param value Header value
     * @return This for chaining
     */
    public RequestMatcherRule withDefaultHeader(String key, String value) {
        defaultHeaders.put(key, value);
        return this;
    }

    /**
     * Sets whether it should be tried to guess the proper mime type for the fixture from its file
     * extension.
     *
     * @param guess True to enable guessing, false otherwise. Default true.
     * @return This for chaining
     */
    public RequestMatcherRule withGuessingMimeTypeFromFixtureExtension(boolean guess) {
        guessMimeType = guess;
        return this;
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

    /**
     * Returns the wrapped {@link MockWebServer} instance
     */
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
     * Used to read fixtures. This combines the fixturesRootFolder with the passed fixturePath to
     * find the file to read.
     *
     * @param fixturePath Relative path
     * @return The contents of the fixture
     */
    public byte[] readBinaryFixture(final String fixturePath) {
        try {
            return IOReader.readBinary(open(fixturesRootFolder + "/" + fixturePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read asset with path " + fixturePath, e);
        }
    }

    /**
     * Adds a fixture to be used during the test case.
     *
     * @param response The {@link MockResponse} to return.
     * @param matcher  The {@link RequestMatchersGroup} instance to use for matching.
     * @return A dsl instance {@link IfRequestMatches} for chaining
     */
    public <T extends RequestMatchersGroup> IfRequestMatches<T> addResponse(MockResponse response,
                                                                            T matcher) {
        return new IfRequestMatches<>(dispatcher.addFixture(response, matcher));
    }

    /**
     * Adds a fixture to be used during the test case.
     *
     * @param response The {@link MockResponse} to return.
     * @return A dsl instance {@link IfRequestMatches} for chaining
     */
    public IfRequestMatches<RequestMatchersGroup> addResponse(MockResponse response) {
        return new IfRequestMatches<>(dispatcher.addFixture(response));
    }

    /**
     * Adds a fixture to be used during the test case.
     *
     * @param fixturePath The path of the fixture inside the fixtures folder.
     * @return A dsl instance {@link IfRequestMatches} for chaining
     */
    public IfRequestMatches<RequestMatchersGroup> addFixture(String fixturePath) {
        return addFixture(200, fixturePath);
    }

    /**
     * Adds a fixture to be used during the test case.
     *
     * @param fixturePath The path of the fixture inside the fixtures folder.
     * @param statusCode  The status of the mocked response.
     * @return A dsl instance {@link IfRequestMatches} for chaining
     */
    public IfRequestMatches<RequestMatchersGroup> addFixture(int statusCode, String fixturePath) {

        final MockResponse mockResponse = new MockResponse()
                .setResponseCode(statusCode)
                .setBody(readFixture(fixturePath));

        if (guessMimeType) {
            final String mimeType = IOReader.mimeTypeFromExtension(fixturePath);

            if (mimeType != null) {
                mockResponse.addHeader("Content-Type", mimeType);
            }
        }

        if (!defaultHeaders.isEmpty()) {
            for (String headerKey : defaultHeaders.keySet()) {
                mockResponse.addHeader(headerKey, defaultHeaders.get(headerKey));
            }
        }

        return addResponse(mockResponse);
    }

    /**
     * Adds a fixture to be used during the test case.
     *
     * @param fixturePath The path of the fixture inside the fixtures fodler.
     * @param matcher     The {@link RequestMatchersGroup} instance to use for matching.
     * @return A dsl instance {@link IfRequestMatches} for chaining
     */
    public <T extends RequestMatchersGroup> IfRequestMatches<T> addFixture(String fixturePath,
                                                                           T matcher) {
        return addResponse(new MockResponse()
                .setResponseCode(200)
                .setBody(readFixture(fixturePath)), matcher);
    }

    /**
     * Adds a fixture to be used during the test case.
     *
     * @param statusCode  The status of the mocked response.
     * @param fixturePath The path of the fixture inside the fixtures fodler.
     * @param matcher     The {@link RequestMatchersGroup} instance to use for matching.
     * @return A dsl instance {@link IfRequestMatches} for chaining
     */
    public <T extends RequestMatchersGroup> IfRequestMatches<T> addFixture(int statusCode,
                                                                           String fixturePath,
                                                                           T matcher) {
        return addResponse(new MockResponse()
                .setResponseCode(statusCode)
                .setBody(readFixture(fixturePath)), matcher);
    }

    /**
     * Public class that eases the DSL reading of the chain. Sometimes all you want is to add a
     * fixture without a matching. When you DO need to match the request, it is easier to read:
     * "adds a fixture for the request that matches these constraints".
     *
     * @param <T> The type that extends from {@link RequestMatchersGroup}
     */
    public static class IfRequestMatches<T extends RequestMatchersGroup> {

        private final T group;

        IfRequestMatches(T group) {
            this.group = group;
        }

        public T ifRequestMatches() {
            return group;
        }
    }

    private void after(Exception exception, boolean success) throws Exception {

        if (dispatcher.getAssertionException() != null) {

            // if there was an exception in the test (for example a NPE) we print the
            // RequestAssertionException before re-throwing the original exception.
            // This might help debug where the test is failing. We can't simply add it as
            // suppressed as it was added only in API 19.
            if (exception != null) {
                Logger.getLogger(RequestMatcherRule.class.getName())
                        .log(Level.SEVERE, "Test threw exception.", exception);
            }

            throw dispatcher.getAssertionException();
        }

        if (!success) {

            if (exception != null) {
                throw exception;
            }

            return;
        }

        if (dispatcher.size() != 0) {
            try {
                final StringBuilder errorHint = new StringBuilder(100);
                for (MatcherDispatcher.ResponseWithMatcher remainingResponse : dispatcher.getResponseSet()) {

                    final String matcher = remainingResponse.getMatcher().toString();
                    final String response = remainingResponse.getResponse().toString();
                    errorHint.append("Not used matcher:       ")
                            .append(matcher)
                            .append("\nwith expected response: ")
                            .append(response)
                            .append("\n---------------------\n");
                }
                fail("There are fixtures that were not used:\n" + errorHint.toString());
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
                Exception exception = null;
                try {
                    base.evaluate();
                    success = true;

                } catch (Exception e) {
                    exception = e;
                } finally {
                    after(exception, success);
                }
            }
        };
    }
}
