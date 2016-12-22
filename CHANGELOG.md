# CHANGELOG

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
