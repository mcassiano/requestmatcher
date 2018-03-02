package br.com.concretesolutions.requestmatcher.test;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import br.com.concretesolutions.requestmatcher.RequestMatcherRule;
import br.com.concretesolutions.requestmatcher.RequestMatchersGroup;
import br.com.concretesolutions.requestmatcher.exception.RequestAssertionException;
import br.com.concretesolutions.requestmatcher.model.HttpMethod;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public abstract class RequestMatcherRuleTest extends BaseTest {

    private final ExpectedException exceptionRule = ExpectedException.none();
    private final RequestMatcherRule server = getRequestMatcherRule();

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
    public void canAssertRequestsOutOfOrder() throws IOException {

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .hasEmptyBody()
                .methodIs(HttpMethod.GET)
                .pathIs("/get");

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .pathIs("/post")
                .headersContain("key", "value")
                .methodIs(HttpMethod.POST)
                .bodyMatches(containsString("\"property\": \"value\""));

        this.request = new Request.Builder()
                .url(server.url("/post"))
                .header("key", "value")
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"property\": \"value\"}"))
                .build();

        Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("property\": \"value\""));

        this.request = new Request.Builder()
                .url(server.url("/get"))
                .get()
                .build();

        resp = client.newCall(request).execute();

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
                .url(server.url("/post"))
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
                .url(server.url("/get")) // different path
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
                .url(server.url("/get?no_key=no_value"))
                .get()
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void canAssertThatHasAQueryString() throws IOException {

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .queriesContain("key", "value");

        this.request = new Request.Builder()
                .url(server.url("/get?key=value")) // different path
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
                .url(server.url("/get")) // different path
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
                .url(server.url("/get")) // different path
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
                .url(server.url("/post"))
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
                .url(server.url("/body"))
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
                .url(server.url("/post?key=value"))
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
                        containsString("headers: map containing [\"key\"->\"value\"]"),
                        containsString(RequestMatchersGroup.BODY_MSG)
                )
        );

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .pathIs("/post")
                .methodIs(HttpMethod.POST)
                .queriesContain("key", "value")
                .headersContain("key", "value")
                .bodyMatches(containsString("\"property\": \"value\""));

        this.request = new Request.Builder()
                .url(server.url("/post?key=value"))
                .header("key", "value")
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"another\": \"someother\"}"))
                .build();

        this.client.newCall(request).execute();
    }

    @Test
    public void failsIfNoBodyWasExpectedButReceivedOne() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("body: (null or an empty string)"),
                        containsString(RequestMatchersGroup.BODY_MSG)
                ));

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .hasEmptyBody();

        this.request = new Request.Builder()
                .url(server.url("/post"))
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"property\": \"value\"}"))
                .build();

        client.newCall(request).execute();
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
                .url(server.url("/post"))
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
    public void canMatchAgainstJsonPath() throws IOException {

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .pathIs("/post")
                .methodIs(HttpMethod.POST)
                .queriesContain("key", "value")
                .headersContain("key", "value")
                .bodyMatches(containsString("\"property\": \"value\""))
                .bodyAsJsonMatches(
                        allOf(
                                isJson(),
                                hasJsonPath("$.property", is("value")),
                                hasJsonPath("$.parent.property", is("another value")),
                                hasJsonPath("$.parent.child[0].key", is(1)),
                                hasJsonPath("$.parent.child[2].key", is(33))
                        )
                );

        this.request = new Request.Builder()
                .url(server.url("/post?key=value"))
                .header("key", "value")
                .post(RequestBody.create(
                        MediaType.parse("application/json"),
                        server.readFixture("request/test_request.json")))
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("property\": \"value\""));
    }

    @Test
    public void failsForUnknownJsonPath() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("is json with json path \"$..[*]\" and is json with json " +
                                "path \"$['unexisting']['path']\" evaluated to is \"value\")"),
                        containsString(RequestMatchersGroup.JSON_MSG)
                ));

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .bodyAsJsonMatches(
                        allOf(
                                isJson(),
                                hasJsonPath("$.unexisting.path", is("value"))
                        )
                );

        this.request = new Request.Builder()
                .url(server.url("/post?key=value"))
                .header("key", "value")
                .post(RequestBody.create(
                        MediaType.parse("application/json"),
                        server.readFixture("request/test_request.json")))
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("property\": \"value\""));
    }

    @Test
    public void failsIfOrderIsDifferent() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("order: is <3>"),
                        containsString(RequestMatchersGroup.ORDER_MSG)
                ));

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .hasEmptyBody()
                .orderIs(3) // will not math
                .methodIs(HttpMethod.GET)
                .pathIs("/get");

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .pathIs("/post")
                .headersContain("key", "value")
                .methodIs(HttpMethod.POST)
                .bodyMatches(containsString("\"property\": \"value\""));

        this.request = new Request.Builder()
                .url(server.url("/post"))
                .header("key", "value")
                .post(RequestBody.create(MediaType.parse("application/json"), "{\"property\": \"value\"}"))
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("property\": \"value\""));

        this.request = new Request.Builder()
                .url(server.url("/get"))
                .get()
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void canMatchHeaderOnlyByKey() throws IOException {

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .pathIs("/post")
                .headersMatches(hasEntry(is("key"), any(String.class)))
                .methodIs(HttpMethod.PUT)
                .bodyMatches(containsString("\"property\": \"value\""));

        this.request = new Request.Builder()
                .url(server.url("/post"))
                .header("key", "kdmlskdfm84fq9o4f083q4fnoalsjn")
                .put(RequestBody.create(MediaType.parse("application/json"), "{\"property\": \"value\"}"))
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("property\": \"value\""));
    }

    @Test
    public void canMatchHeaderOnlyByValue() throws IOException {

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .pathIs("/post")
                .headersMatches(hasEntry(any(String.class), is("value")))
                .methodIs(HttpMethod.PUT)
                .bodyMatches(containsString("\"property\": \"value\""));

        this.request = new Request.Builder()
                .url(server.url("/post"))
                .header("kdmlskdfm84fq9o4f083q4fnoalsjn", "value")
                .put(RequestBody.create(MediaType.parse("application/json"), "{\"property\": \"value\"}"))
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("property\": \"value\""));
    }

    @Test
    public void canEnsureThereAreNoQueries() throws IOException {

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .hasNoQueries();

        this.request = new Request.Builder()
                .url(server.url("/get"))
                .get()
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("property\": \"value\""));
    }

    @Test
    public void failIfExpectedNoQueriesButGotSome() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectMessage(
                allOf(
                        containsString("query parameters: a map with size <0>"),
                        containsString(RequestMatchersGroup.QUERIES_MSG)
                ));

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .hasNoQueries()
                .hasEmptyBody();

        this.request = new Request.Builder()
                .url(server.url("/get?key=value"))
                .get()
                .build();

        client.newCall(request).execute();
    }

    @Test
    public void ensureFixtureHasContentTypeAttachedAutomaticallyForJson() throws IOException {

        server.addFixture(200, "body.json");

        this.request = new Request.Builder()
                .url(server.url("/get"))
                .get()
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.headers().get("Content-Type"), is("application/json"));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(),
                containsString("property\": \"value\""));
    }

    @Test
    public void ensureFixtureHasContentTypeAttachedAutomaticallyForXml() throws IOException {

        server.addFixture(200, "body.xml");

        this.request = new Request.Builder()
                .url(server.url("/get"))
                .get()
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.headers().get("Content-Type"), is("text/xml"));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("<xml />"));
    }

    @Test
    public void canAddADefaultHeader() throws IOException {

        server.withDefaultHeader("anykey", "anyvalue");

        server.addFixture(200, "body.xml")
                .ifRequestMatches()
                .orderIs(1);

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .orderIs(2);

        this.request = new Request.Builder()
                .url(server.url("/"))
                .get()
                .build();

        Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.headers().get("Content-Type"), is("text/xml"));
        assertThat(resp.headers().get("anykey"), is("anyvalue"));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("<xml />"));

        resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.headers().get("Content-Type"), is("application/json"));
        assertThat(resp.headers().get("anykey"), is("anyvalue"));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(),
                containsString("property\": \"value\""));
    }

    @Test
    public void canEnableOrDisableGuessingOfMimeType() throws IOException {

        server.withGuessingMimeTypeFromFixtureExtension(false);

        server.addFixture(200, "body.xml");

        this.request = new Request.Builder()
                .url(server.url("/get"))
                .get()
                .build();

        Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.headers().get("Content-Type"), is(nullValue()));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("<xml />"));

        server.withGuessingMimeTypeFromFixtureExtension(true);

        server.addFixture(200, "body.xml");

        resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.headers().get("Content-Type"), is("text/xml"));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("<xml />"));
    }

    @Test
    public void canAssertGETRequests() throws IOException {

        server.addFixture(200, "body.json")
                .ifRequestMatches()
                .methodIs(HttpMethod.GET)
                .pathMatches(is("/get"));

        this.request = new Request.Builder()
                .url(server.url("/get"))
                .get()
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(), containsString("property\": \"value\""));
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
                .url(server.url("/binary"))
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
                .url(server.url("/body/0"))
                .post(RequestBody.create(MediaType.parse("application/json"), jsonRequestBody0))
                .build();
        Request request1 = new Request.Builder()
                .url(server.url("/body/1"))
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
                .url(server.url(path))
                .get()
                .build();
        Request request1 = new Request.Builder()
                .url(server.url(path))
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
                .url(server.url(path))
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

        client.newCall(request0).execute(); // will fail with message of non used matchers
    }

    @Test
    public void canReplaceValuesInTemplate() throws IOException {

        String timestamp = String.valueOf(new Date(1519775413753L).getTime());

        server.addTemplate("json_template.json")
                .withValueForKey("current_date", timestamp)
                .ifRequestMatches();

        this.request = new Request.Builder()
                .url(server.url("/get"))
                .get()
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.isSuccessful(), is(true));
        assertThat(resp.headers().get("Content-Type"), is("application/json"));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(),
                containsString("\"current_date\": \"1519775413753\""));
    }

    @Test
    public void canReplaceValuesInTemplateWithNonSuccessfulResponse() throws IOException {

        server.addTemplate(404, "json_error_template.json")
                .withValueForKey("error", "Resource not found")
                .ifRequestMatches();

        this.request = new Request.Builder()
                .url(server.url("/get"))
                .get()
                .build();

        final Response resp = client.newCall(request).execute();

        assertThat(resp.code() == 404, is(true));
        assertThat(resp.headers().get("Content-Type"), is("application/json"));
        assertThat(resp.peekBody(1_000_000).source().readUtf8(),
                containsString("\"error_message\": \"Resource not found\""));
    }

    @Test
    public void failsIfTemplateDoesNotHaveGivenKeys() throws IOException {

        exceptionRule.expect(AssertionError.class);
        exceptionRule.expectMessage(containsString("Could not find any template key named something"));

        server.addTemplate("body.json")
                .withValueForKey("something", "something")
                .ifRequestMatches();

        this.request = new Request.Builder()
                .url(server.url("/get"))
                .get()
                .build();

        client.newCall(request).execute();
    }


}
