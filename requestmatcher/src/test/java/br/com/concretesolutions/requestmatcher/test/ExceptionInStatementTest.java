package br.com.concretesolutions.requestmatcher.test;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import br.com.concretesolutions.requestmatcher.BuildConfig;
import br.com.concretesolutions.requestmatcher.LocalTestRequestMatcherRule;
import br.com.concretesolutions.requestmatcher.RequestMatcherRule;
import br.com.concretesolutions.requestmatcher.exception.RequestAssertionException;
import br.com.concretesolutions.requestmatcher.model.HttpMethod;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class ExceptionInStatementTest {

    private final ExpectedException exceptionRule = ExpectedException.none();
    private final RequestMatcherRule server = new LocalTestRequestMatcherRule();

    @Rule
    public TestRule chain = RuleChain
            .outerRule(exceptionRule)
            .around(server);

    private OkHttpClient client;
    private Request request;

    @Before
    public void setUp() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(2_000, TimeUnit.SECONDS)
                .readTimeout(2_000, TimeUnit.SECONDS)
                .build();
    }

    @Test
    public void ensureAProgrammingExceptionInTestDoesNotShadowAssertion() throws IOException, InterruptedException {

        exceptionRule.expect(RequestAssertionException.class);

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .pathIs("/get")
                .methodIs(HttpMethod.POST)
                .queriesContain("key", "value"); // fail

        this.request = new Request.Builder()
                .url(server.url("/get").toString())
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"body\":\"a body\"}"))
                .build();

        client.newCall(request).execute();

        Object nullRef = null;
        // throws NullPointerException in statement!!!
        nullRef.equals("");
    }

    @Test
    public void ensureNoMatchersMessageIsReadable() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("methodMatcher = is <DELETE>"),
                        containsString("methodMatcher = is <POST>"),
                        containsString("methodMatcher = is <PUT>"),
                        containsString("methodMatcher = is <GET>"),
                        containsString("methodMatcher = is <PATCH>,\n" +
                                "\torderMatcher = is <2>")
                ));

        server.addFixture(201, "body.json").ifRequestMatches().methodIs(HttpMethod.GET);
        server.addFixture(201, "body.json").ifRequestMatches().methodIs(HttpMethod.POST);
        server.addFixture(201, "body.json").ifRequestMatches().methodIs(HttpMethod.PUT);
        server.addFixture(201, "body.json").ifRequestMatches().methodIs(HttpMethod.DELETE);
        server.addFixture(201, "body.json").ifRequestMatches().methodIs(HttpMethod.PATCH).orderIs(2);

        this.request = new Request.Builder()
                .url(server.url("/head").toString())
                .patch(RequestBody.create(MediaType.parse("application/json"), "{\"property\": \"value\"}"))
                .build();

        // throws RequestAssertionException expected in after clause
        client.newCall(request).execute();
    }
}
