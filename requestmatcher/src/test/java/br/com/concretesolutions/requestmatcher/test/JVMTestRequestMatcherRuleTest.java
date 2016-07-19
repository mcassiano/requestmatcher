package br.com.concretesolutions.requestmatcher.test;

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

import br.com.concretesolutions.requestmatcher.BuildConfig;
import br.com.concretesolutions.requestmatcher.JVMTestRequestMatcherRule;
import br.com.concretesolutions.requestmatcher.RequestAssertionException;
import br.com.concretesolutions.requestmatcher.RequestMatcher;
import br.com.concretesolutions.requestmatcher.RequestMatcherRule;
import br.com.concretesolutions.requestmatcher.assertion.BodyAssertion;
import br.com.concretesolutions.requestmatcher.assertion.RequestAssertion;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class JVMTestRequestMatcherRuleTest {

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

    @Test
    public void canAssertGETRequests() throws IOException {

        server.enqueueGET(200, "body.json")
                .assertNoBody()
                .assertPathIs("/get");

        this.request = new Request.Builder()
                .url(server.url("/get").toString())
                .get()
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("property\": \"value\""));
    }

    @Test
    public void failsIfExpectedNoBodyButOneWasProvided() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(containsString("Expected no body but received one"));

        server.enqueue(200, "body.json")
                .assertNoBody(); // NO BODY

        this.request = new Request.Builder()
                .url(server.url("/post").toString())
                .post(RequestBody.create(MediaType.parse("application/json"), "{}")) // YES BODY
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void failsIfExpectedPathIsDifferent() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("Expected: is \"/post\""),
                        containsString("but: was \"/get\"")));

        server.enqueue(200, "body.json")
                .assertPathIs("/post");

        this.request = new Request.Builder()
                .url(server.url("/get").toString()) // different path
                .get()
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void failsIfExpectedQueryDoesNotExist() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("Expected: a collection containing <Query{key=value}>"),
                        containsString("but: was <Query{no_key=no_value}>")));

        server.enqueue(200, "body.json")
                .assertHasQuery("key", "value");


        this.request = new Request.Builder()
                .url(server.url("/get?no_key=no_value").toString())
                .get()
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void canAssertThatHasAQueryString() throws IOException {

        server.enqueue(200, "body.json")
                .assertHasQuery("key", "value");

        this.request = new Request.Builder()
                .url(server.url("/get?key=value").toString()) // different path
                .get()
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("property\": \"value\""));
    }

    @Test
    public void canAssertThatHasAHeader() throws IOException {

        server.enqueue(200, "body.json")
                .assertHasHeader("key", "value");

        this.request = new Request.Builder()
                .url(server.url("/get").toString()) // different path
                .header("key", "value")
                .get()
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("property\": \"value\""));
    }

    @Test
    public void failsIfExpectedHeaderDoesNotExist() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("Expected: map containing [\"key\"-><[value]>]"),
                        containsString("but: map was [")));

        server.enqueue(200, "body.json")
                .assertHasHeader("key", "value");

        this.request = new Request.Builder()
                .url(server.url("/get").toString()) // different path
                .get()
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void canAssertThatHasAProperBody() throws IOException {

        server.enqueue(200, "body.json")
                .assertBody(new BodyAssertion() {
                    @Override
                    public void doAssert(String body) {
                        assertThat(body, containsString("\"property\": \"value\""));
                    }
                });

        this.request = new Request.Builder()
                .url(server.url("/post").toString())
                .header("key", "value")
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"property\": \"value\"}"))
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("property\": \"value\""));
    }

    @Test
    public void failsIfBodyAssertionFails() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("Expected: a string containing \"\\\"property\\\": \\\"value\\\"\""),
                        containsString("but: was \"{\"another\": \"someother\"}\"")));

        server.enqueue(200, "body.json")
                .assertBody(new BodyAssertion() {
                    @Override
                    public void doAssert(String body) {
                        assertThat(body, containsString("\"property\": \"value\""));
                    }
                });

        this.request = new Request.Builder()
                .url(server.url("/body").toString())
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"another\": \"someother\"}"))
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void canMakeSeveralAssertions() throws IOException {

        server.enqueue(200, "body.json")
                .assertPathIs("/post")
                .assertMethodIs(RequestMatcher.POST)
                .assertHasQuery("key", "value")
                .assertHasHeader("key", "value")
                .assertBody(new BodyAssertion() {
                    @Override
                    public void doAssert(String body) {
                        assertThat(body, containsString("\"property\": \"value\""));
                    }
                });

        this.request = new Request.Builder()
                .url(server.url("/post?key=value").toString())
                .header("key", "value")
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"property\": \"value\"}"))
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("property\": \"value\""));
    }

    @Test
    public void failIfAnyOfTheAssertionsFail() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("Expected: a string containing \"\\\"property\\\": \\\"value\\\"\""),
                        containsString("but: was \"{\"another\": \"someother\"}\"")));

        server.enqueue(200, "body.json")
                .assertPathIs("/post")
                .assertMethodIs(RequestMatcher.POST)
                .assertHasQuery("key", "value")
                .assertHasHeader("key", "value")
                .assertBody(new BodyAssertion() {
                    @Override
                    public void doAssert(String body) {
                        assertThat(body, containsString("\"property\": \"value\""));
                    }
                });

        this.request = new Request.Builder()
                .url(server.url("/post?key=value").toString())
                .header("key", "value")
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"another\": \"someother\"}"))
                .build();

        this.client.newCall(request).execute();
    }

    @Test
    public void canAssertWithWholeRecordedRequest() throws IOException {

        server.enqueue(200, "body.json")
                .assertRequest(new RequestAssertion() {
                    @Override
                    public void doAssert(RecordedRequest request) {
                        assertThat(request.getMethod(), is(RequestMatcher.POST));
                    }
                });

        this.request = new Request.Builder()
                .url(server.url("/post?key=value").toString())
                .header("key", "value")
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"property\": \"value\"}"))
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("property\": \"value\""));
    }

    @Test
    public void failsIfRequestAssertionFails() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("Expected: is \"GET\""),
                        containsString("but: was \"POST\"")));

        server.enqueue(200, "body.json")
                .assertRequest(new RequestAssertion() {
                    @Override
                    public void doAssert(RecordedRequest request) {
                        assertThat(request.getMethod(), is(RequestMatcher.GET));
                    }
                });

        this.request = new Request.Builder()
                .url(server.url("/post?key=value").toString())
                .header("key", "value")
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"property\": \"value\"}"))
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void failsIfNoBodyWasExpectedButReceivedOne() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(containsString("Expected no body but received one"));

        server.enqueue(200, "body.json").assertNoBody();

        this.request = new Request.Builder()
                .url(server.url("/post").toString())
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"property\": \"value\"}"))
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void failsIfNoQueryWasPassedButExpectedOne() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(containsString("Expected query strings but found none"));

        server.enqueue(200, "body.json")
                .assertHasQuery("key", "value");

        this.request = new Request.Builder()
                .url(server.url("/post").toString())
                .get()
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void failsIfHelperGETMethodFails() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("Expected: is \"GET\""),
                        containsString("but: was \"POST\"")));

        server.enqueueGET(200, "body.json");

        this.request = new Request.Builder()
                .url(server.url("/post").toString())
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"property\": \"value\"}"))
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void failsIfHelperPOSTMethodFails() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("Expected: is \"POST\""),
                        containsString("but: was \"GET\"")));

        server.enqueuePOST(200, "body.json");

        this.request = new Request.Builder()
                .url(server.url("/get").toString())
                .get()
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void failsIfHelperPUTMethodFails() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("Expected: is \"PUT\""),
                        containsString("but: was \"GET\"")));

        server.enqueuePUT(200, "body.json");

        this.request = new Request.Builder()
                .url(server.url("/get").toString())
                .get()
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void failsIfEnqueuedRequestsAreNotUsed() {
        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(containsString("Failed assertion. There are enqueued requests that were not used."));
        server.enqueuePUT(200, "body.json");
    }
}
