package br.com.concretesolutions.requestmatcher;

import java.io.IOException;
import java.io.InputStream;

public class UnitTestRequestMatcherRule extends RequestMatcherRule {
    @Override
    protected InputStream open(String path) throws IOException {
        return UnitTestRequestMatcherRule.class.getClassLoader().getResourceAsStream(path);
    }
}
