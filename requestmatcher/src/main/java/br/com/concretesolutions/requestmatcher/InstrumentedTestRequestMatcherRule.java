package br.com.concretesolutions.requestmatcher;

import android.support.test.InstrumentationRegistry;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.mockwebserver.MockWebServer;

/**
 * A rule for instrumented tests.
 */
public class InstrumentedTestRequestMatcherRule extends RequestMatcherRule {

    public InstrumentedTestRequestMatcherRule() {
    }

    public InstrumentedTestRequestMatcherRule(String fixturesRootFolder) {
        super(fixturesRootFolder);
    }

    public InstrumentedTestRequestMatcherRule(MockWebServer server) {
        super(server);
    }

    public InstrumentedTestRequestMatcherRule(MockWebServer server, String fixturesRootFolder) {
        super(server, fixturesRootFolder);
    }

    @Override
    protected InputStream open(String path) throws IOException {
        return InstrumentationRegistry.getContext().getAssets().open(path);
    }
}
