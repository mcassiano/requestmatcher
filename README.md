# RequestMatcher

[![Build Status](https://circleci.com/gh/concretesolutions/requestmatcher.png?circle-token=b28528473dcb6c076cc1b91a3023f411fc78790a)](https://circleci.com/gh/concretesolutions/requestmatcher/tree/master)

A simple and powerful way for making assertions in your mocked API.

To properly test an Android application we must isolate all the external dependencies that we can't control. Normally, in a client/server application, this boils down to the API calls.

There are several approaches to mocking the server interaction:

- Dependency Injection: use a test version of your server interaction component.
- Evil API: have a test implementation of your API contracts (implement the interface for tests).
- Use specific mocking libraries: if you use `Retrofit` you can use `Retrofit-mock` which gives you easier ways to set up your mocking implementation.

All of the above makes testing APIs possible though not highly configurable on a *per test* basis. There is another (and probably many others) approach:

- Use a mock web server: a real server responding to requests that you set up on your test to the expected behaviour.

This approach is nice though may generate lots of code in your tests to setup proper request assertions. This library tries to simplify that and add some other automatic detection of wrong doings in tests setup.

## Download

The library is available in JCenter repositories. To use it, just declare it in your app's build.gradle:

``` groovy
dependencies {

    // unit tests
    testCompile "br.com.concretesolutions:requestmatcher:$latestVersion"

    // instrumented tests
    androidTestCompile "br.com.concretesolutions:requestmatcher:$latestVersion"
}
```

This library depends on the following libraries:

- [MockWebServer](https://github.com/square/okhttp/tree/master/mockwebserver): this is the mock server implementation from Square.
- [JUnit 4](http://junit.org/): the test runner library
- [Hamcrest](http://hamcrest.org/JavaHamcrest/): a generic Java assert library
- [Json Path (optional for Json matching)](https://github.com/jayway/JsonPath)

So, ensure those libraries are also in your dependencies. For example:

``` groovy
dependencies {

    // unit tests
    testCompile "br.com.concretesolutions:requestmatcher:$latestVersion"
    testCompile 'junit:junit:4.12'
    testCompile 'org.hamcrest:hamcrest-all:1.3'
    testCompile "com.squareup.okhttp3:mockwebserver:3.4.1"
    testCompile 'com.jayway.jsonpath:json-path-assert:2.2.0' // optional

    // instrumented tests
    androidTestCompile "br.com.concretesolutions:requestmatcher:$latestVersion"
    androidTestCompile "com.squareup.okhttp3:mockwebserver:3.4.1"
    androidTestCompile "com.android.support.test.espresso:espresso-core:2.2.2" // this already has hamcrest
    androidTestCompile "com.android.support.test:runner:0.5" // this already has junit
    androidTestCompile 'com.jayway.jsonpath:json-path-assert:2.2.0' // optional
}
```

## Usage

The core API of this library is centered around the class `RequestMatcherRule`. This is a wrapping rule around Square's `MockWebServer`. A basic unit test can be setup like:

``` java
public class UnitTest {

    @Rule
    public final RequestMatcherRule serverRule = new LocalTestRequestMatcherRule();

    @Before
    public void setUp() {
        // Setup your application to point to this rootUrl
        final String rootUrl = serverRule.url("/").toString();

        // do setup
    }

    @Test
    public void canMakeRequestAssertions() {

        serverRule.addFixture(200, "body.json")
            .ifRequestMatches()
            .pathIs("/somepath")
            .hasEmptyBody()
            .methodIs(HttpMethod.GET);

        // make interaction with the server
    }
}
```

In this example, several things are checked:

- The test *MUST* make exclusively *ONE* request to the mock server or else it will fail.
- The request must be a GET or else it will fail.
- The request must not contain a body or else it will fail.
- The request must target path `rootUrl + "/somepath"` or else it will fail.

We think this declarative way of making assertions on requests will make tests more consistent with expected behaviour.

### Adding `MockResponse`s

To add a `MockResponse` all you have to do is call one of the `addResponse` methods from the server rule.

``` java
serverRule.addResponse(new MockResponse().setResponseCode(500));
```

### Adding fixtures

To add a fixture all you have to do is call one of the `addFixture` methods in the `RequestMatcherRule`. That means you can save your mocks in a folder and load them up while you are mocking the API. Example:

``` java
serverRule.addFixture(200, "body.json");
```

This will add a response with status code 200 and the contents of the file `body.json` as the body. This file, by default, must be located in a folder with name `fixtures`. This folder works different for Unit Tests and Instrumented Tests.

- Unit tests: these are run locally in the JVM (usually with Robolectric) and follow different conventions. Your source folder `test` may contain a folder `java` and a fodler `resources`. When you compile your code it takes everything in the `resources` folder and puts in the root of your `.class` files. So, your fixtures folder must go inside `resources` folder.
- Instrumented tests: there are run in a device or emulator (usually with Espresso or Robotium). It follows the android fodler layout and so you may have an assets folder inside your `androidTest` folder. Your `fixtures` folder must go there.

Because of these differences, there are two implementations of `RequestMatcherRule`: `LocalTestRequestMatcherRule` and `InstrumentedTestRequestMatcherRule`. You should use the generic type for your variable and instantiate it with the required type. Example:

``` java
// Unit Test
@Rule
public final RequestMatcherRule server = new LocalTestRequestMatcherRule();

// or

// Instrumented Test
@Rule
public final RequestMatcherRule server = new InstrumentedTestRequestMatcherRule();
```

The difference is that when we run an InstrumentedTest, we must pass the instrumentation context (and *NOT* the target context).

## Configuring the `RequestMatcherRule`

It is possible to pass some parameters to the server rule's constructor:

- `MockWebServer` server: an instance of the MockWebServer to use instead a default one.  
- `String` fixturesRootFolder: the name of the folder in the corresponding context. Defaults to 'fixtures'.

## RequestAssertionException

When an assertion fails, it throws a `RequestAssertionException`. Of course, this happens in the server thread and so, if we throw an exception from there the client will hang and most likely receive a timeout. This would make tests last too long an dconsequently the test suite. To avoid this, the assertion is buffered and the response is delivered as if it were disconnected. The response is like the snippet below:

``` java
new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_END);
```

## Request Matching

This rule provides a DSL for matching against requests. You can and should provide matchers against each part of a request. See the base `RequestMatchersGroup` for all possible matching.  

## Examples

``` java
server.addFixture(200, "body.json")
            .ifRequestMatches()
            .pathIs("/post") // path must be "/post"
            .headersMatches(hasEntry(any(String.class), is("value"))) // some header must contain value "value"
            .methodIs(HttpMethod.PUT) // method must be PUT
            .bodyMatches(containsString("\"property\": \"value\"")); // body must contain the string passed
```

## Custom `RequestMatcher`

The library is flexible enough for customizing the `RequestMatcherGroup` implementation you want to use. To do that, use the method `addResponse(MockResponse response, T matcher)`, `addFixture(String path, T matcher)` or `addFixture(int statusCode, String fixturePath, T matcher)` where `matcher` is an instance of any class that extends `RequestMatchersGroup`.

With that you can provide your own assertions, for example, you can create assertions according to some custom protocol. This is more useful for those not following strict RESTful architectures.

Example:

``` java
CustomMatcher matcher = server.addResponse(new MockResponse().setBody("Some body"), new CustomMatcher()).ifRequestMatches();
```

## Other examples

For more examples, please check the tests in the library module and the sample module.

## LICENSE

This project is available under Apache Public License version 2.0. See [LICENSE](LICENSE).
