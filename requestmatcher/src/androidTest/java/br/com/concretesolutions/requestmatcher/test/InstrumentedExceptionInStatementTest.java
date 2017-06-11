package br.com.concretesolutions.requestmatcher.test;

import android.support.test.runner.AndroidJUnit4;

import org.junit.runner.RunWith;

import br.com.concretesolutions.requestmatcher.InstrumentedTestRequestMatcherRule;
import br.com.concretesolutions.requestmatcher.RequestMatcherRule;

@RunWith(AndroidJUnit4.class)
public class InstrumentedExceptionInStatementTest extends ExceptionInStatementTest {

    @Override
    protected RequestMatcherRule getRequestMatcherRule() {
        return new InstrumentedTestRequestMatcherRule();
    }
}
