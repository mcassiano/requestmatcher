package br.com.concrete.requestmatcher;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.mockwebserver.MockWebServer;

/**
 * A {@link RequestMatcherRule} for tests that run on the JVM (locally).
 *
 * For reference: https://github.com/concretesolutions/requestmatcher/wiki/Local-or-Instrumented-tests
 */
public class LocalTestRequestMatcherRule extends RequestMatcherRule {

    /**
     * Creates a rule with a new instance of {@link MockWebServer}. This will by default look for
     * fixtures in the "fixtures" folder.
     */
    public LocalTestRequestMatcherRule() {}

    /**
     * Creates a rule with a new instance of {@link MockWebServer}.
     *
     * @param fixturesRootFolder The root folder to look for fixtures. Defaults to "fixtures"
     */
    public LocalTestRequestMatcherRule(String fixturesRootFolder) {
        super(fixturesRootFolder);
    }

    /**
     * Creates a rule with the given instance of {@link MockWebServer}. This will by default look
     * for fixtures in the "fixtures" folder.
     *
     * @param server The {@link MockWebServer} instance
     */
    public LocalTestRequestMatcherRule(MockWebServer server) {
        super(server);
    }

    /**
     * Creates a rule with the given instance of {@link MockWebServer}. This will by default look
     * for fixtures in the "fixtures" folder.
     *
     * @param server             The {@link MockWebServer} instance
     * @param fixturesRootFolder The root folder to look for fixtures. Defaults to "fixtures"
     */
    public LocalTestRequestMatcherRule(MockWebServer server, String fixturesRootFolder) {
        super(server, fixturesRootFolder);
    }

    @Override
    protected InputStream open(String path) throws IOException {
        return LocalTestRequestMatcherRule.class.getClassLoader().getResourceAsStream(path);
    }
}
