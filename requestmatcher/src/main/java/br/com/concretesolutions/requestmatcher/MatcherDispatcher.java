package br.com.concretesolutions.requestmatcher;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.concretesolutions.requestmatcher.exception.NoMatchersForRequestException;
import br.com.concretesolutions.requestmatcher.exception.RequestAssertionException;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

public final class MatcherDispatcher extends Dispatcher {

    private static final Logger logger = Logger.getLogger(MatcherDispatcher.class.getName());
    private static final String ASSERT_HEADER = "REQUEST-ASSERT";
    private static final String DEFAULT_MESSAGE = "Unexpected exception during assertion.";

    private final AtomicInteger order = new AtomicInteger();
    private final Set<ResponseWithMatcher> responseSet = Collections.newSetFromMap(
            new ConcurrentHashMap<ResponseWithMatcher, Boolean>()
    );

    private RequestAssertionException assertionError;

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {

        final int currentOrder = order.incrementAndGet();

        int matcherOrder = 0;
        final NoMatchersForRequestException.Builder builder =
                new NoMatchersForRequestException.Builder(request);

        for (ResponseWithMatcher response : responseSet) {

            final RequestMatchersGroup matcher = response.getMatcher();

            if (matcher != null) {
                try {
                    matcher.doAssert(request, currentOrder);
                    responseSet.remove(response);
                    return response.getResponse(); // return proper response
                } catch (AssertionError assertionError) {
                    builder.appendAssertionError(++matcherOrder, assertionError, matcher);
                    // continue
                } catch (Exception e) {
                    this.assertionError = new RequestAssertionException(DEFAULT_MESSAGE, e);
                    logger.log(Level.SEVERE, "Error while doing assert", e);
                    return response.getResponse(); // return response but keep exception
                }
            }
        }

        // noinspection ThrowableInstanceNeverThrown
        this.assertionError = new RequestAssertionException(DEFAULT_MESSAGE, builder.build());
        return new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_END);
    }

    public RequestAssertionException getAssertionException() {
        return assertionError;
    }

    public int size() {
        return responseSet.size();
    }

    public RequestMatchersGroup addFixture(MockResponse response) {
        return addFixture(response, new RequestMatchersGroup());
    }

    public <T extends RequestMatchersGroup> T addFixture(MockResponse response, T requestMatcher) {
        final String assertPath = response.hashCode() + "::" + System.identityHashCode(requestMatcher);
        responseSet.add(new ResponseWithMatcher(requestMatcher, response.setHeader(ASSERT_HEADER, assertPath)));
        return requestMatcher;
    }

    public static class ResponseWithMatcher {
        private final MockResponse response;
        private final RequestMatchersGroup matcher;

        ResponseWithMatcher(RequestMatchersGroup matcher, MockResponse response) {
            this.matcher = matcher;
            this.response = response;
        }

        MockResponse getResponse() {
            return response;
        }

        public RequestMatchersGroup getMatcher() {
            return matcher;
        }
    }
}
