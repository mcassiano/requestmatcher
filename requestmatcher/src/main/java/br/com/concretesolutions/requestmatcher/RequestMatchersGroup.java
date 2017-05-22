package br.com.concretesolutions.requestmatcher;

import android.support.annotation.NonNull;

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

    public static final String METHOD_MSG = "METHOD did NOT match.";
    public static final String PATH_MSG = "PATH did NOT match.";
    public static final String QUERIES_MSG = "QUERY PARAMETERS did NOT match.";
    public static final String HEADERS_MSG = "HEADERS did NOT match.";
    public static final String BODY_MSG = "BODY did NOT match.";
    public static final String JSON_MSG = "JSON BODY did NOT match.";
    public static final String ORDER_MSG = "REQUEST ORDER did NOT match.";

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
    public void doAssert(@NonNull final RecordedRequest request, final int currentOrder) {

        if (methodMatcher != null) {
            assertThat(METHOD_MSG, HttpMethod.forRequest(request), methodMatcher);
        }

        if (pathMatcher != null) {
            assertThat(PATH_MSG, RequestUtils.getPathOnly(request), pathMatcher);
        }

        final String path = request.getPath();

        if (queryMatcher != null) {

            if (!path.contains("?")) {
                assertThat(QUERIES_MSG, new HashMap<String, String>(0), queryMatcher);
            } else {
                assertThat(QUERIES_MSG, RequestUtils.buildQueryMap(path), queryMatcher);
            }
        }

        if (headersMatcher != null) {
            assertThat(HEADERS_MSG,
                    RequestUtils.buildHeadersMap(request.getHeaders()), headersMatcher);
        }

        // clone the body! perhaps we need the request body for a future assertion.
        final String body = request.getBody().clone().readUtf8();

        if (bodyMatcher != null) {
            assertThat(BODY_MSG, body, bodyMatcher);
        }

        if (jsonMatcher != null) {
            assertThat(JSON_MSG, body, jsonMatcher);
        }

        if (orderMatcher != null) {
            assertThat(ORDER_MSG, currentOrder, orderMatcher);
        }
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
        if (target != null) {
            throw new IllegalStateException(message);
        }
    }

    public StringBuilder buildExpectedMatchers(@NonNull final StringBuilder sb) {

        sb.append("Request matchers group:\n");

        if (methodMatcher != null) {
            sb.append(" - method: ").append(methodMatcher).append('\n');
        }

        if (pathMatcher != null) {
            sb.append(" - path: ").append(pathMatcher).append('\n');
        }

        if (queryMatcher != null) {
            sb.append(" - query parameters: ").append(queryMatcher).append('\n');
        }

        if (headersMatcher != null) {
            sb.append(" - headers: ").append(headersMatcher).append('\n');
        }

        if (bodyMatcher != null) {
            sb.append(" - body: ").append(bodyMatcher).append('\n');
        }

        if (jsonMatcher != null) {
            sb.append(" - JSON body: ").append(jsonMatcher).append('\n');
        }

        if (orderMatcher != null) {
            sb.append(" - request order: ").append(orderMatcher).append('\n');
        }

        return sb;
    }

    @Override
    public String toString() {
        return new StringBuilder("RequestMatchersGroup{bodyMatcher=").append(bodyMatcher)
                .append(", pathMatcher=").append(pathMatcher)
                .append(", methodMatcher=").append(methodMatcher)
                .append(", orderMatcher=").append(orderMatcher)
                .append(", queryMatcher=").append(queryMatcher)
                .append(", headersMatcher=").append(headersMatcher)
                .append(", jsonMatcher=").append(jsonMatcher)
                .append('}').toString();
    }
}
