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
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import br.com.concretesolutions.requestmatcher.BuildConfig;
import br.com.concretesolutions.requestmatcher.LocalTestRequestMatcherRule;
import br.com.concretesolutions.requestmatcher.MatcherDispatcher;
import br.com.concretesolutions.requestmatcher.RequestMatcherRule;
import br.com.concretesolutions.requestmatcher.RequestMatchersGroup;
import br.com.concretesolutions.requestmatcher.exception.NoMatchersForRequestException;
import br.com.concretesolutions.requestmatcher.exception.RequestAssertionException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.Matchers.is;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class CustomRequestMatcherTest {

    private final ExpectedException exceptionRule = ExpectedException.none();
    private final RequestMatcherRule server = new LocalTestRequestMatcherRule();

    @Rule
    public TestRule chain = RuleChain
            .outerRule(exceptionRule)
            .around(server);

    private OkHttpClient client;
    private final NoMatchersForRequestException expectedAssertionError =
            new NoMatchersForRequestException(Collections.<MatcherDispatcher.ResponseWithMatcher>emptySet());

    class CustomMatcher extends RequestMatchersGroup {

        boolean shouldThrow;

        CustomMatcher assertThatItThrows() {
            this.shouldThrow = true;
            return this;
        }

        @Override
        public void doAssert(RecordedRequest request) {
            super.doAssert(request);

            if (shouldThrow) {
                throw expectedAssertionError;
            }
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
        server.addResponse(new MockResponse().setBody("plain body"), new CustomMatcher());
        server.addResponse(new MockResponse().setBody("plain body"), new CustomMatcher())
                .ifRequestMatches()
                .assertThatItThrows(); // ensure it throws

        final Request request = new Request.Builder()
                .url(server.url("/get").toString())
                .get()
                .build();

        client.newCall(request).execute();
        client.newCall(request).execute(); // throws on second time
    }
}
