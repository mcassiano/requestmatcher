package br.com.concrete.requestmatcher.test;

import br.com.concrete.requestmatcher.LocalTestRequestMatcherRule;
import br.com.concrete.requestmatcher.RequestMatcherRule;

//@RunWith(RobolectricTestRunner.class)
//@Config(constants = BuildConfig.class, sdk = 23)
public class LocalExceptionInStatementTest extends ExceptionInStatementTest {

    @Override
    protected RequestMatcherRule getRequestMatcherRule() {
        return new LocalTestRequestMatcherRule();
    }
}
