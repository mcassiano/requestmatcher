package br.com.concrete.requestmatcher.test;

import br.com.concrete.requestmatcher.LocalTestRequestMatcherRule;
import br.com.concrete.requestmatcher.RequestMatcherRule;

public class LocalExceptionWitouhtEnqueuesTest extends ExceptionWitouhtEnqueuesTest {

    @Override
    protected RequestMatcherRule getRequestMatcherRule() {
        return new LocalTestRequestMatcherRule();
    }
}
