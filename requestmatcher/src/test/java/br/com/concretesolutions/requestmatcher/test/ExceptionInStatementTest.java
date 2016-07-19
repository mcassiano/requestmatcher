package br.com.concretesolutions.requestmatcher.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import br.com.concretesolutions.requestmatcher.JVMTestRequestMatcherRule;
import br.com.concretesolutions.requestmatcher.RequestAssertionException;
import br.com.concretesolutions.requestmatcher.RequestMatcherRule;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = br.com.concretesolutions.requestmatcher.BuildConfig.class, sdk = 23)
public class ExceptionInStatementTest {

    public final ExpectedException exceptionRule = ExpectedException.none();
    public final RequestMatcherRule server = new JVMTestRequestMatcherRule();

    @Rule
    public TestRule chain = RuleChain
            .outerRule(exceptionRule)
            .around(server);

    private OkHttpClient client;
    private Request request;

    @Before
    public void setUp() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void ensureAProgrammingExceptionInTestDoesNotShadowAssertion() throws IOException, InterruptedException {

        server.enqueueGET(200, "body.json")
                .assertNoBody()
                .assertPathIs("/get");

        this.request = new Request.Builder()
                .url(server.url("/get").toString())
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"body\":\"a body\"}"))
                .build();

        client.newCall(request).execute();

        Object nullRef = null;
        // throws NullPointerException in statement!!!
        nullRef.equals("");
    }

    @After
    public void afterAssertion() {
        exceptionRule.expect(RequestAssertionException.class);
    }
}
