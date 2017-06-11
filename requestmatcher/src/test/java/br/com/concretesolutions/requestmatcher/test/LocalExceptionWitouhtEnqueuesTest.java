package br.com.concretesolutions.requestmatcher.test;

import br.com.concretesolutions.requestmatcher.LocalTestRequestMatcherRule;
import br.com.concretesolutions.requestmatcher.RequestMatcherRule;

public class LocalExceptionWitouhtEnqueuesTest extends ExceptionWitouhtEnqueuesTest {

    @Override
    protected RequestMatcherRule getRequestMatcherRule() {
        return new LocalTestRequestMatcherRule();
    }
}
