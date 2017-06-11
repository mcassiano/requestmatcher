package br.com.concretesolutions.requestmatcher.test;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.io.IOException;

import br.com.concretesolutions.requestmatcher.RequestMatcherRule;

public abstract class ExceptionWitouhtEnqueuesTest extends BaseTest {

    private final ExpectedException exceptionRule = ExpectedException.none();
    private final RequestMatcherRule server = getRequestMatcherRule();

    @Rule
    public TestRule chain = RuleChain
            .outerRule(exceptionRule)
            .around(server);

    @Test
    public void ensureAProgrammingExceptionInTestIsPorperlyPropagated() throws IOException, InterruptedException {

        exceptionRule.expect(NullPointerException.class);
        Object nullRef = null;
        // throws NullPointerException in statement!!!
        nullRef.equals("");
    }
}
