# OkHttpClient Task Implementation

This module provides a task implementation for the [OkHttp](https://square.github.io/okhttp/) library.

## Installation

Add the following dependency to your `maven` file:

```xml

<dependency>
    <groupId>com.orquex.sagas</groupId>
    <artifactId>orquex-sagas-task-okhttp</artifactId>
    <version>${orquex-sagas.version}</version>
</dependency>
```

## Configuration

All `OkHttp` implementations requires an instance of `OkHttpClient` to be passed to its constructor.

### Task Configuration

```json
{
  "id": "okhttp-get-activity",
  "name": "OkHttp GET activity implementation",
  "implementation": "okhttp-get",
  "metadata": {
    "__client_provider": "oauth2",
    "__url": "https://example.com/posts/1",
    "__params": {},
    "__headers": {}
  }
}
```

### Payload Request

```json
{
  "url": "",
  "params": {},
  "headers": {},
  "body": {}
}
```
