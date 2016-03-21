# RequestMatcher

A simple and powerful way for making assertions in your mocked API.

To properly test an Android Application we must isolate all the external dependencies that we can't control. Normally, in a client/server application, this boils down to the API calls. 

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
    testCompile "br.com.concretesolutions:requestmatcher:1.0.0-alpha

    // instrumented tests
    androidTestCompile "br.com.concretesolutions:requestmatcher:1.0.0-alpha
}
```

This library depends on the following libraries:

- [MockWebServer](https://github.com/square/okhttp/tree/master/mockwebserver): this is the mock server implementation from Square.
- [JUnit 4](http://junit.org/): the test runner library
- [Hamcrest](http://hamcrest.org/JavaHamcrest/): a generic Java assert library

So, ensure those libraries are also in your dependencies. For example:

``` groovy
dependencies {
    
    // unit tests
    testCompile "br.com.concretesolutions:requestmatcher:1.0.0-alpha"
    testCompile 'junit:junit:4.12'
    testCompile 'org.hamcrest:hamcrest-all:1.3'
    testCompile "com.squareup.okhttp3:mockwebserver:3.2.0"

    // instrumented tests
    androidTestCompile "br.com.concretesolutions:requestmatcher:1.0.0-alpha"
    androidTestCompile "com.squareup.okhttp3:mockwebserver:3.2.0"
    androidTestCompile "com.android.support.test.espresso:espresso-core:2.2.2" // this already has hamcrest
    androidTestCompile "com.android.support.test:runner:0.5" // this already has junit
}
```

## Usage

The core API of this library is centered around the class `RequestMatcherRule`. This is a wrapping rule around Square's `MockWebServer`. A basic unit test can be setup like:

``` java
public class UnitTest {

    @Rule
    public final RequestMatcherRule serverRule = new UnitTestRequestMatcherRule();

    @Before
    public void setUp() {
        // Setup your application to point to this rootUrl
        final String rootUrl = serverRule.url("/").toString(); 
        
        // do setup
    }
    
    @Test
    public void canMakeRequestAssertions() {
        
        serverRule.enqueue(200, "body.json")
            .assertPathIs("/somepath")
            .assertNoBody()
            .assertMethodId(RequestMatcher.GET);
            
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

### Enqueing responses

To enqueue a response all you have to do is call one of the enqueue methods in the `RequestMatcherRule`. Some of them have the ability to read a fixture built-in. That means you can save your mocks in a folder and load them up while you are enqueuing. Example:

``` java
serverRule.enqueue(200, "body.json")
```

This will enqueue a response with status code 200 and the contents of the file `body.json` as the body. This file *MUST* be located in a folder with name `fixtures`. This folder works different for Unit Tests and Instrumented Tests.

- Unit tests: these are run in the JVM (usually with Robolectric) and follow different conventions. Your source folder `test` may contain a folder `java` and a fodler `resources`. When you compile your code it takes everything in the `resources` folder and puts in the root of your `.class` files. So, your fixtures folder must go inside `resources` folder.
- Instrumented tests: there are run in a device or emulator (usually with Espresso or Robotium). It follows the android fodler layout and so you may have an assets folder inside your `androidTest` folder. Your `fixtures` folder must go there.

Because of these differences, there are two implementations of `RequestMatcherRule`: `UnitTestRequestMatcherRule` and `InstrumentedTestRequestMatcherRule`. You should use the generic type for your variable and instantiate it with the required type. Example:

``` java
// Unit Test
@Rule
public RequestMatcherRule server = new UnitTestRequestMatcherRule();

// or

// Instrumented Test
@Rule
public RequestMatcherRule server = new InstrumentedTestRequestMatcherRule(InstrumentationRegistry.getContext());
```

The difference is that when we run an InstrumentedTest, we must pass the instrumentation context (and *NOT* the target context).

## RequestAssertionError

When an assertion fails, it throws a `RequestAssertionError`. Of course, this happens in the server thread and so, if we throw an exception from there the client will hang and most likely receive a timeout. This would make tests last too long an dconsequently the test suite. To avoid this, the assertion is buffered and the response is delivered as if it were disconnected. The response is like the snippet below:

``` java
new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_END);
```

## Request Assertions

### .assertPathIs(String path)

Ensures path is the one expected.

### .assertMethodIs(String method)

Ensures the request method is the one expected. You can use the helper `RequestMatcher.GET`, `RequestMatcher.POST`, `RequestMatcher.PUT` and `RequestMatcher.DELETE`.

### .assertHasQuery(String key, String value)

Ensures the request contains a query string with expected key and value.

### .assertHasHeader(String key, String value)

Ensures the request contains a header with expected key and value.

### .assertBody(BodyAssertion bodyAssertion)

Gives access to the body through a SAM (single abstract method) interface. You can make any assertion inside the `doAssert(String body)` method.

### .assertNoBody()

Ensures request does not have a body.

### .assertRequest(RequestAssertion requestAssertion)

Gives access to the whole request in a SAM interface. You can make any assertion in there.

## LICENSE

This project is available under Apache Public License version 2.0. See [LICENSE](LICENSE).
