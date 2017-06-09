package br.com.concretesolutions.requestmatcher.test;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import br.com.concretesolutions.requestmatcher.InstrumentedTestRequestMatcherRule;
import br.com.concretesolutions.requestmatcher.RequestMatcherRule;
import br.com.concretesolutions.requestmatcher.RequestMatchersGroup;
import br.com.concretesolutions.requestmatcher.exception.RequestAssertionException;
import br.com.concretesolutions.requestmatcher.model.HttpMethod;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(AndroidJUnit4.class)
public class InstrumentedTestRequestMatcherRuleTest {

    private ExpectedException exceptionRule = ExpectedException.none();
    private RequestMatcherRule server = new InstrumentedTestRequestMatcherRule();

    @Rule
    public RuleChain chain = RuleChain
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

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .methodIs(HttpMethod.GET)
                .pathMatches(is("/get"));

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
        exceptionRule.expectMessage(
                allOf(
                        containsString("body: (null or an empty string)"),
                        containsString(RequestMatchersGroup.BODY_MSG)
                ));

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .hasEmptyBody(); // NO BODY

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
                        containsString("path: is \"/post\""),
                        containsString(RequestMatchersGroup.PATH_MSG)
                ));

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .pathIs("/post");

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
                        containsString("query parameters: map containing [\"key\"->\"value\"]"),
                        containsString(RequestMatchersGroup.QUERIES_MSG)
                ));

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .queriesContain("key", "value");

        this.request = new Request.Builder()
                .url(server.url("/get?no_key=no_value").toString())
                .get()
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void canAssertThatHasAQueryString() throws IOException {

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .queriesMatches(hasEntry("key", "value"));

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

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .headersContain("key", "value");

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
                        containsString("headers: map containing [\"key\"->\"value\"]"),
                        containsString(RequestMatchersGroup.HEADERS_MSG)
                ));

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .headersContain("key", "value");

        this.request = new Request.Builder()
                .url(server.url("/get").toString()) // different path
                .get()
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void canAssertThatHasAProperBody() throws IOException {

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .bodyMatches(containsString("\"property\": \"value\""));

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
                        containsString("body: a string containing \"\\\"property\\\": \\\"value\\\"\""),
                        containsString(RequestMatchersGroup.BODY_MSG)
                ));

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .bodyMatches(containsString("\"property\": \"value\""));

        this.request = new Request.Builder()
                .url(server.url("/body").toString())
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"another\": \"someother\"}"))
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void canMakeSeveralAssertions() throws IOException {

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .pathIs("/post")
                .methodIs(HttpMethod.POST)
                .queriesContain("key", "value")
                .headersContain("key", "value")
                .bodyMatches(containsString("\"property\": \"value\""));

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
                        containsString("body: a string containing \"\\\"property\\\": \\\"value\\\"\""),
                        containsString("path: is \"/post\""),
                        containsString("method: is <POST>"),
                        containsString("query parameters: map containing [\"key\"->\"value\"]"),
                        containsString("headers: map containing [\"key\"->\"value\"]")
                ));

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .pathIs("/post")
                .methodIs(HttpMethod.POST)
                .queriesContain("key", "value")
                .headersContain("key", "value")
                .bodyMatches(containsString("\"property\": \"value\""));

        this.request = new Request.Builder()
                .url(server.url("/post?key=value").toString())
                .header("key", "value")
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"another\": \"someother\"}"))
                .build();

        this.client.newCall(request).execute();
    }

    @Test
    public void failsIfNoQueryWasPassedButExpectedOne() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("query parameters: map containing [\"key\"->\"value\"]"),
                        containsString(RequestMatchersGroup.QUERIES_MSG)
                ));

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .queriesContain("key", "value");

        this.request = new Request.Builder()
                .url(server.url("/post").toString())
                .get()
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void failsIfEnqueuedRequestsAreNotUsed() {
        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("There are fixtures that were not used:"),
                        containsString("Not used matcher:"),
                        containsString("RequestMatchersGroup{"),
                        containsString("pathMatcher=is \"/somepath\"")));


        server.addFixture(200, "body.json").ifRequestMatches().pathIs("/somepath");
    }

    @Test
    public void canReadBinaryFixture() {
        final byte[] bytes = server.readBinaryFixture("screenshot.png");
        assertThat(bytes, is(notNullValue()));
        assertThat(bytes.length, is(63_537));
    }

    @Test
    public void canDetectMimeTypeFromBinaryFixture() throws IOException {

        server.addFixture(200, "screenshot.png")
                .ifRequestMatches()
                .pathIs("/binary");

        this.request = new Request.Builder()
                .url(server.url("/binary").toString())
                .get()
                .build();

        final Response response = client.newCall(request).execute();

        assertThat(response.code(), is(200));
        assertThat(response.header("Content-type"), is("image/png"));
    }

    @Test
    public void canAssertRequestBodyMultipleTimes() throws IOException {

        String jsonRequestBody0 = "{\"key\" : 0}";
        String jsonRequestBody1 = "{\"key\" : 1}";

        Request request0 = new Request.Builder()
                .url(server.url("/body/0").toString())
                .post(RequestBody.create(MediaType.parse("application/json"), jsonRequestBody0))
                .build();
        Request request1 = new Request.Builder()
                .url(server.url("/body/1").toString())
                .post(RequestBody.create(MediaType.parse("application/json"), jsonRequestBody1))
                .build();

        // MatcherDispatcher uses a Set for this responses. So it is hard to ensure that
        // matcher 1 is ordered first... but exactly this must be tested - the body of
        // request1 must be read two times!
        // the set implementation seems to keep the insertion order...
        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .orderIs(1)
                .bodyMatches(equalTo(jsonRequestBody0));
        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .orderIs(2)
                .bodyMatches(equalTo(jsonRequestBody1));

        client.newCall(request0).execute();
        client.newCall(request1).execute();
    }

    @Test
    public void canKeepOrderOfFixtures() throws IOException {
        // prepare
        String path = "/same/path";
        Request request0 = new Request.Builder()
                .url(server.url(path).toString())
                .get()
                .build();
        Request request1 = new Request.Builder()
                .url(server.url(path).toString())
                .get()
                .build();

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .orderIs(2)
                .pathIs(path);
        server.addFixture(201, "body.json") // <- code = 201 must be the first one!
                .ifRequestMatches()
                .orderIs(1)
                .pathIs(path);

        // execute
        Response response0 = client.newCall(request0).execute();
        Response response1 = client.newCall(request1).execute();

        // verify
        assertThat(response0.code(), is(201));
        assertThat(response1.code(), is(200));
    }

    @Test
    public void informsAboutNotUsedFixture() throws IOException {
        // prepare
        String path = "/same/path";
        Request request0 = new Request.Builder()
                .url(server.url(path).toString())
                .get()
                .build();
        server.addFixture(201, "body.json")
                .ifRequestMatches()
                .orderIs(1)
                .pathIs(path);
        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .orderIs(2)
                .pathIs(path);
        server.addFixture(500, "body.json")
                .ifRequestMatches()
                .hasEmptyBody()
                .pathIs("/some/different/path");

        // execute & verify
        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(containsString("200"));
        exceptionRule.expectMessage(containsString("/some/different/path"));

        Response response0 = client.newCall(request0).execute();
    }
}
