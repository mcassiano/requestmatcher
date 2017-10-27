package br.com.concrete.requestmatcher.test;

import android.support.annotation.NonNull;

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
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import br.com.concrete.requestmatcher.BuildConfig;
import br.com.concrete.requestmatcher.LocalTestRequestMatcherRule;
import br.com.concrete.requestmatcher.RequestMatcherRule;
import br.com.concrete.requestmatcher.RequestMatchersGroup;
import br.com.concrete.requestmatcher.exception.NoMatchersForRequestException;
import br.com.concrete.requestmatcher.exception.RequestAssertionException;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private NoMatchersForRequestException expectedAssertionError;

    class CustomMatcher extends RequestMatchersGroup {

        boolean shouldThrow;

        CustomMatcher assertThatItThrows() {
            this.shouldThrow = true;
            return this;
        }

        @Override
        public void doAssert(@NonNull RecordedRequest request, int currentOrder) {
            super.doAssert(request, currentOrder);

            if (shouldThrow) {
                throw expectedAssertionError;
            }
        }
    }

    @Before
    public void setUp() throws IOException {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build();

        final Buffer buffer = new Buffer();
        RequestBody
                .create(MediaType.parse("application/json"), "{}")
                .writeTo(buffer);

        final InetAddress inetAddressMock = mock(InetAddress.class);
        when(inetAddressMock.getHostName()).thenReturn("localhost");

        final Socket mock = mock(Socket.class);
        when(mock.getInetAddress()).thenReturn(inetAddressMock);

        final RecordedRequest recordedRequest = new RecordedRequest(
                "GET / ",
                Headers.of("Content-type", "application/json"),
                null,
                2,
                buffer,
                0,
                mock);

        expectedAssertionError = new NoMatchersForRequestException.Builder(recordedRequest)
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
