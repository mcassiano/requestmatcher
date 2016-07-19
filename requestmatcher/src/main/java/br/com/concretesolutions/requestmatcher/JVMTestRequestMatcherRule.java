package br.com.concretesolutions.requestmatcher;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.mockwebserver.MockWebServer;

/**
 * A {@link RequestMatcherRule} for tests that run on the JVM.
 */
public class JVMTestRequestMatcherRule extends RequestMatcherRule {

    public JVMTestRequestMatcherRule() {}

    public JVMTestRequestMatcherRule(String fixturesRootFolder) {
        super(fixturesRootFolder);
    }

    public JVMTestRequestMatcherRule(MockWebServer server) {
        super(server);
    }

    public JVMTestRequestMatcherRule(MockWebServer server, String fixturesRootFolder) {
        super(server, fixturesRootFolder);
    }

    @Override
    protected InputStream open(String path) throws IOException {
        return JVMTestRequestMatcherRule.class.getClassLoader().getResourceAsStream(path);
    }
}
