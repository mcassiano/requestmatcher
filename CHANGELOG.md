# CHANGELOG

## 2.1.0

### Non-backwards compatible changes

- Custom `RequestMatchersGroup` must implement the order check and assertions on the same method.
 
 ```java
 void doAssert(@NonNull final RecordedRequest request, @NonNull final int currentOrder)
 ```

### Features

- New enhanced exception message on assertion error. Example:

```
Unexpected exception during assertion. No matcher found for request: 

> PATCH /head HTTP/1.1
> Content-Type: application/json; charset=utf-8
> Content-Length: 21
> Host: localhost:52271
> Connection: Keep-Alive
> Accept-Encoding: gzip
> User-Agent: okhttp/3.4.1

{"property": "value"}

Tried the following matchers:

1. Request matchers group:
 - method: is <DELETE>

 Failed because METHOD did NOT match.
 Expected: is <DELETE>
      but: was <PATCH>

2. Request matchers group:
 - method: is <PATCH>
 - request order: is <2>

 Failed because REQUEST ORDER did NOT match.
 Expected: is <2>
      but: was <1>

3. Request matchers group:
 - method: is <PUT>

 Failed because METHOD did NOT match.
 Expected: is <PUT>
      but: was <PATCH>

4. Request matchers group:
 - method: is <GET>

 Failed because METHOD did NOT match.
 Expected: is <GET>
      but: was <PATCH>

5. Request matchers group:
 - method: is <POST>

 Failed because METHOD did NOT match.
 Expected: is <POST>
      but: was <PATCH>

6. Request matchers group:
 - method: is <PATCH>
 - query parameters: is map containing [is "key"->an instance of java.lang.String]

 Failed because QUERY PARAMETERS did NOT match.
 Expected: is map containing [is "key"->an instance of java.lang.String]
      but: map was []
```

### Bugfixes

- Multiple body assertions do not consume the response body. Thanks to @cleemansen. 

## 2.0.0

- Major refactoring non-backwards compatible. Sticking to this API from now on.

### Features

- Real matcher style assertions
- New matching strategy: ensures first match wins before order, though, order can be asserted too.
- JSON matching for body using [JSON Path](https://github.com/jayway/JsonPath) (optional dependency)
- Changed JVM naming strategy to Local as indicated in Google's documentation
- Added default headers to `RequestMatcherRule`
- Added configurable mime type detection from fixture path extension (.json -> appplication/json and so on). By default we try to guess.
- Added Checkstyle, PMD and Jacoco (this still needs some fixing) to the project. Fixed issues raised by those.

## 1.0.0 (18/07/2016)

- Initial release
