package br.com.concrete.requestmatcher.test;

import br.com.concrete.requestmatcher.LocalTestRequestMatcherRule;
import br.com.concrete.requestmatcher.RequestMatcherRule;

public class LocalRequestMatcherRuleTest extends RequestMatcherRuleTest {

    @Override
    protected RequestMatcherRule getRequestMatcherRule() {
        return new LocalTestRequestMatcherRule();
    }
}
