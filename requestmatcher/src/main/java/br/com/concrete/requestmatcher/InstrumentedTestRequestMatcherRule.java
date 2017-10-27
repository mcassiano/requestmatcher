package br.com.concrete.requestmatcher;

import android.support.test.InstrumentationRegistry;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.mockwebserver.MockWebServer;

/**
 * A {@link RequestMatcherRule} for tests that run on devices or emulators.
 *
 * For reference: https://github.com/concretesolutions/requestmatcher/wiki/Local-or-Instrumented-tests
 */
public class InstrumentedTestRequestMatcherRule extends RequestMatcherRule {

    /**
     * Creates a rule with a new instance of {@link MockWebServer}. This will by default look for
     * fixtures in the "fixtures" folder.
     */
    public InstrumentedTestRequestMatcherRule() {}

    /**
     * Creates a rule with a new instance of {@link MockWebServer}.
     *
     * @param fixturesRootFolder The root folder to look for fixtures. Defaults to "fixtures"
     */
    public InstrumentedTestRequestMatcherRule(String fixturesRootFolder) {
        super(fixturesRootFolder);
    }

    /**
     * Creates a rule with the given instance of {@link MockWebServer}. This will by default look
     * for fixtures in the "fixtures" folder.
     *
     * @param server The {@link MockWebServer} instance
     */
    public InstrumentedTestRequestMatcherRule(MockWebServer server) {
        super(server);
    }

    /**
     * Creates a rule with the given instance of {@link MockWebServer}. This will by default look
     * for fixtures in the "fixtures" folder.
     *
     * @param server             The {@link MockWebServer} instance
     * @param fixturesRootFolder The root folder to look for fixtures. Defaults to "fixtures"
     */
    public InstrumentedTestRequestMatcherRule(MockWebServer server, String fixturesRootFolder) {
        super(server, fixturesRootFolder);
    }

    @Override
    protected InputStream open(String path) throws IOException {
        return InstrumentationRegistry.getContext().getAssets().open(path);
    }
}
