# HttpClient Task Implementation

This module provides task implementations for HTTP operations using the Apache HttpClient library within workflow definitions.

## Installation

Add the following dependency to your Maven `pom.xml`:

```xml
<dependency>
    <groupId>co.orquex.sagas</groupId>
    <artifactId>orquex-sagas-task-http-client</artifactId>
    <version>${orquex-sagas.version}</version>
</dependency>
```

## Supported Implementations

The following HTTP methods are supported:

- `http-get`: Performs a GET request
- `http-post`: Performs a POST request with a body
- `http-put`: Performs a PUT request with a body
- `http-patch`: Performs a PATCH request with a body
- `http-delete`: Performs a DELETE request

## Task Configuration

Each HTTP task is configured with metadata and payload in JSON format.

### Metadata Fields

- `__client_provider`: The name of the registered HTTP client provider (e.g., "basic-client", "oauth2")
- `__url`: The target URL for the HTTP request
- `__params`: Optional query parameters as a map (e.g., `{"key": "value"}`)
- `__headers`: Optional request headers as a map (e.g., `{"Authorization": "Bearer token"}`)

### Payload Fields

- `url`: Optional override for the URL (takes precedence over metadata `__url`)
- `params`: Optional query parameters (merged with metadata `__params`)
- `headers`: Optional headers (merged with metadata `__headers`)
- `body`: Request body as a map (required for POST, PUT, PATCH; ignored for GET/DELETE)

## Examples

### GET Request

```json
{
  "id": "http-get-activity",
  "name": "Fetch user data",
  "implementation": "http-get",
  "metadata": {
    "__client_provider": "basic-client",
    "__url": "https://api.example.com/users/1",
    "__headers": {
      "Accept": "application/json"
    }
  }
}
```

### POST Request

```json
{
  "id": "http-post-activity",
  "name": "Create new user",
  "implementation": "http-post",
  "metadata": {
    "__client_provider": "oauth2",
    "__url": "https://api.example.com/users",
    "__headers": {
      "Content-Type": "application/json"
    }
  },
  "payload": {
    "body": {
      "name": "John Doe",
      "email": "john@example.com"
    }
  }
}
```

### PUT Request

```json
{
  "id": "http-put-activity",
  "name": "Update user",
  "implementation": "http-put",
  "metadata": {
    "__client_provider": "basic-client",
    "__url": "https://api.example.com/users/1"
  },
  "payload": {
    "body": {
      "name": "Jane Doe"
    }
  }
}
```

### PATCH Request

```json
{
  "id": "http-patch-activity",
  "name": "Partially update user",
  "implementation": "http-patch",
  "metadata": {
    "__client_provider": "oauth2",
    "__url": "https://api.example.com/users/1"
  },
  "payload": {
    "body": {
      "email": "jane@example.com"
    }
  }
}
```

### DELETE Request

```json
{
  "id": "http-delete-activity",
  "name": "Delete user",
  "implementation": "http-delete",
  "metadata": {
    "__client_provider": "basic-client",
    "__url": "https://api.example.com/users/1"
  }
}
```
