package br.com.concretesolutions.requestmatcher;

import java.net.HttpURLConnection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import br.com.concretesolutions.requestmatcher.model.MatcherMockResponse;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;

public class MatcherQueueDispatcher extends Dispatcher {

    private static final Logger logger = Logger.getLogger(MatcherQueueDispatcher.class.getName());
    private static final String
            ASSERT_HEADER = "REQUEST-ASSERT",
            ERROR_MESSAGE = "Failed assertion for %s";

    protected final BlockingQueue<MatcherMockResponse> responseQueue = new LinkedBlockingQueue<>();
    private final MockResponse disconnectResponse = new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START);
    private MockResponse failFastResponse;
    private RequestAssertionException assertionError;

    public BlockingQueue<MatcherMockResponse> getQueue() {
        return responseQueue;
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        // To permit interactive/browser testing, ignore requests for favicons.
        final String requestLine = request.getRequestLine();
        if (requestLine != null && requestLine.equals("GET /favicon.ico HTTP/1.1")) {
            logger.info("served " + requestLine);
            return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND);
        }

        if (failFastResponse != null && responseQueue.peek() == null) {
            // Fail fast if there's no response queued up.
            return failFastResponse;
        }

        final MatcherMockResponse response = responseQueue.take();
        final RequestMatcher matcher = response.getMatcher();

        if (matcher != null)
            try {
                matcher.doAssert(request);
            } catch (AssertionError e) {
                this.assertionError = new RequestAssertionException(String.format(ERROR_MESSAGE, request), e);
                return new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_END);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error while doing assert", e);
                return response.getResponse();
            }

        return response.getResponse();
    }

    @Override
    public MockResponse peek() {
        MatcherMockResponse peek = responseQueue.peek();
        if (peek != null) return peek.getResponse();
        if (failFastResponse != null) return failFastResponse;
        return super.peek();
    }

    public void setFailFast(boolean failFast) {
        MockResponse failFastResponse = failFast
                ? new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
                : null;
        setFailFast(failFastResponse);
    }

    public void setFailFast(MockResponse failFastResponse) {
        this.failFastResponse = failFastResponse;
    }

    public RequestMatcher enqueue(MockResponse response) {
        return enqueue(response, new RequestMatcher());
    }

    public <T extends RequestMatcher> T enqueue(MockResponse response, T requestMatcher) {
        final String assertPath = response.hashCode() + "::" + System.identityHashCode(requestMatcher);
        responseQueue.add(new MatcherMockResponse(requestMatcher, response.setHeader(ASSERT_HEADER, assertPath)));
        return requestMatcher;
    }

    public RequestMatcher enqueueDisconnect() {
        return enqueue(disconnectResponse);
    }

    public RequestAssertionException getAssertionException() {
        return assertionError;
    }
}
