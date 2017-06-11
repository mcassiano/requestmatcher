package br.com.concretesolutions.requestmatcher.test;

import br.com.concretesolutions.requestmatcher.LocalTestRequestMatcherRule;
import br.com.concretesolutions.requestmatcher.RequestMatcherRule;

//@RunWith(RobolectricTestRunner.class)
//@Config(constants = BuildConfig.class, sdk = 23)
public class LocalExceptionInStatementTest extends ExceptionInStatementTest {

    @Override
    protected RequestMatcherRule getRequestMatcherRule() {
        return new LocalTestRequestMatcherRule();
    }
}
