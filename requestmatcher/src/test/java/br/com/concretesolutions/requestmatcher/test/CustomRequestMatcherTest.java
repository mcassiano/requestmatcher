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

import br.com.concretesolutions.requestmatcher.JVMTestRequestMatcherRule;
import br.com.concretesolutions.requestmatcher.RequestAssertionException;
import br.com.concretesolutions.requestmatcher.RequestMatcher;
import br.com.concretesolutions.requestmatcher.RequestMatcherRule;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.Matchers.is;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = br.com.concretesolutions.requestmatcher.BuildConfig.class, sdk = 23)
public class CustomRequestMatcherTest {

    public final ExpectedException exceptionRule = ExpectedException.none();
    public final RequestMatcherRule server = new JVMTestRequestMatcherRule();

    @Rule
    public TestRule chain = RuleChain
            .outerRule(exceptionRule)
            .around(server);

    private OkHttpClient client;
    private final AssertionError expectedAssertionError = new AssertionError("Fail");

    private class CustomMatcher extends RequestMatcher {

        private boolean shouldThrow;

        public CustomMatcher assertThatItThrows() {
            this.shouldThrow = true;
            return this;
        }

        @Override
        public void doAssert(RecordedRequest request) {
            super.doAssert(request);

            if (shouldThrow)
                throw expectedAssertionError;
        }
    }

    @Before
    public void setUp() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build();
    }

    @Test
    public void canPassADifferentRequestMatcherToEnqueueThatThrowsException() throws IOException {

        exceptionRule.expect(RequestAssertionException.class);
        exceptionRule.expectCause(is(expectedAssertionError));

        // Enqueue 2 times
        server.enqueue(new MockResponse().setBody("plain body"), new CustomMatcher());
        server.enqueue(new MockResponse().setBody("plain body"), new CustomMatcher())
                .assertThatItThrows();

        final Request request = new Request.Builder()
                .url(server.url("/get").toString())
                .get()
                .build();

        client.newCall(request).execute();
        client.newCall(request).execute(); // throws on second time
    }
}
