package br.com.concrete.requestmatcher.test;

import android.support.test.runner.AndroidJUnit4;

import org.junit.runner.RunWith;

import br.com.concrete.requestmatcher.InstrumentedTestRequestMatcherRule;
import br.com.concrete.requestmatcher.RequestMatcherRule;

@RunWith(AndroidJUnit4.class)
public class InstrumentedRequestMatcherRuleTest extends RequestMatcherRuleTest {

    @Override
    protected RequestMatcherRule getRequestMatcherRule() {
        return new InstrumentedTestRequestMatcherRule();
    }
}
