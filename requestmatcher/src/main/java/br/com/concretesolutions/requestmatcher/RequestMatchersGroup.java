package br.com.concretesolutions.requestmatcher;

import org.hamcrest.Matcher;

import java.util.HashMap;
import java.util.Map;

import br.com.concretesolutions.requestmatcher.model.HttpMethod;
import okhttp3.mockwebserver.RecordedRequest;

import static br.com.concretesolutions.requestmatcher.matchers.IsMapWithSize.anEmptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;

/**
 * A set of matchers that are run in a {@link RecordedRequest}.
 */
public class RequestMatchersGroup {

    private Matcher<String> bodyMatcher;
    private Matcher<String> pathMatcher;
    private Matcher<HttpMethod> methodMatcher;
    private Matcher<Integer> orderMatcher;
    private Matcher<Map<? extends String, ? extends String>> queryMatcher;
    private Matcher<Map<? extends String, ? extends String>> headersMatcher;
    private Matcher<Object> jsonMatcher;

    /**
     * Main assert method called in the {@link okhttp3.mockwebserver.MockWebServer} dispatching.
     */
    public void doAssert(RecordedRequest request) {

        if (methodMatcher != null)
            assertThat(HttpMethod.forRequest(request), methodMatcher);

        if (pathMatcher != null)
            assertThat(RequestUtils.getPathOnly(request), pathMatcher);

        final String path = request.getPath();

        if (queryMatcher != null) {

            if (!path.contains("?"))
                assertThat(new HashMap<String, String>(0), queryMatcher);
            else
                assertThat(RequestUtils.buildQueryMap(path), queryMatcher);
        }

        if (headersMatcher != null)
            assertThat(RequestUtils.buildHeadersMap(request.getHeaders()), headersMatcher);

        final String body = request.getBody().readUtf8();

        if (bodyMatcher != null)
            assertThat(body, bodyMatcher);

        if (jsonMatcher != null)
            assertThat(body, jsonMatcher);
    }

    public void assertOrder(int currentOrder) {
        if (orderMatcher != null)
            assertThat(currentOrder, orderMatcher);
    }

    public RequestMatchersGroup hasEmptyBody() {
        checkIsNull(bodyMatcher, "Body assertion is already set");
        bodyMatcher = isEmptyOrNullString();
        return this;
    }

    public RequestMatchersGroup hasNoQueries() {
        checkIsNull(queryMatcher, "Query assertion is already set");
        queryMatcher = anEmptyMap();
        return this;
    }

    public RequestMatchersGroup pathIs(String path) {
        checkIsNull(pathMatcher, "Path assertion is already set");
        pathMatcher = is(path);
        return this;
    }

    public RequestMatchersGroup methodIs(HttpMethod method) {
        checkIsNull(methodMatcher, "Method assertion is already set");
        methodMatcher = is(method);
        return this;
    }

    public RequestMatchersGroup orderIs(int order) {
        checkIsNull(orderMatcher, "Order assertion is already set");
        orderMatcher = is(order);
        return this;
    }

    public RequestMatchersGroup queriesContain(String queryKey, String queryValue) {
        queryMatcher = queryMatcher != null
                ? allOf(hasEntry(queryKey, queryValue), queryMatcher)
                : hasEntry(queryKey, queryValue);
        return this;
    }

    public RequestMatchersGroup headersContain(String headerKey, String headerValue) {
        headersMatcher = headersMatcher != null
                ? allOf(hasEntry(headerKey, headerValue), headersMatcher)
                : hasEntry(headerKey, headerValue);
        return this;
    }

    public RequestMatchersGroup pathMatches(Matcher<String> pathMatcher) {
        checkIsNull(this.pathMatcher, "Path assertion is already set");
        this.pathMatcher = pathMatcher;
        return this;
    }

    public RequestMatchersGroup methodMatches(Matcher<HttpMethod> methodMatcher) {
        checkIsNull(this.methodMatcher, "Method assertion is already set");
        this.methodMatcher = methodMatcher;
        return this;
    }

    public RequestMatchersGroup queriesMatches(Matcher<Map<? extends String, ? extends String>> queryMatcher) {
        this.queryMatcher = this.queryMatcher != null
                ? allOf(queryMatcher, this.queryMatcher)
                : queryMatcher;
        return this;
    }

    public RequestMatchersGroup headersMatches(Matcher<Map<? extends String, ? extends String>> headersMatcher) {
        this.headersMatcher = this.headersMatcher != null
                ? allOf(headersMatcher, this.headersMatcher)
                : headersMatcher;
        return this;
    }

    public RequestMatchersGroup bodyMatches(Matcher<String> bodyMatcher) {
        this.bodyMatcher = this.bodyMatcher != null
                ? allOf(bodyMatcher, this.bodyMatcher)
                : bodyMatcher;
        return this;
    }

    public RequestMatchersGroup bodyAsJsonMatches(Matcher<Object> jsonMatcher) {
        this.jsonMatcher = this.jsonMatcher != null
                ? allOf(jsonMatcher, this.jsonMatcher)
                : jsonMatcher;
        return this;
    }

    private void checkIsNull(Object target, String message) {
        if (target != null)
            throw new IllegalStateException(message);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RequestMatchersGroup{");

        if (bodyMatcher != null) sb.append("\n\tbodyMatcher = ").append(bodyMatcher);
        if (jsonMatcher != null) sb.append("\n\tjsonMatcher = ").append(jsonMatcher);
        if (pathMatcher != null) sb.append(",\n\tpathMatcher = ").append(pathMatcher);
        if (methodMatcher != null) sb.append(",\n\tmethodMatcher = ").append(methodMatcher);
        if (orderMatcher != null) sb.append(",\n\torderMatcher = ").append(orderMatcher);
        if (queryMatcher != null) sb.append(",\n\tqueryMatcher = ").append(queryMatcher);
        if (headersMatcher != null) sb.append(",\n\theadersMatcher = ").append(headersMatcher);

        sb.append("\n}");
        return sb.toString();
    }
}
