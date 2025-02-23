# Orquex Sagas Task - JSONata4Java

This task allows evaluating JSONata expressions using the [JSONata4Java](https://github.com/IBM/JSONata4Java) library during the execution of a workflow. This is useful when you need to transform the input or output of a task.

For information about JSONata, visit [JSONata](https://docs.jsonata.org/overview.html)

## Usage

### Maven dependency

```xml
<dependency>
    <groupId>co.orquex.sagas</groupId>
    <artifactId>orquex-sagas-task</artifactId>
    <version>{version}</version>
</dependency>
```

The task expects the following `metadata` parameters:

- `__expression`: The JSONata expression to evaluate in base64 format.

In the payload response, the task will return a `Map<String, Serializable>` with the result of the evaluation, if the evaluation is a single value, this will be returned with the key `__result`.

e.g.
```
{
  "__result": "" | false | 0 | null
}
```

### Task Configuration

```json
{
  "id": "jsonata",
  "name": "JSONata activity task",
  "implementation": "jsonata4j"
}
```

### ActivityTask Configuration

e.g. Expression

```text
{
  "headers": {
    "Authorization": 'Bearer ' & context.__accessKey
  }
}
```

The activity task with the expression in base64 format

```json
{
  "task": "jsonata",
  "metadata": {
    "__expression": "ewogICJoZWFkZXJzIjogewogICAgIkF1dGhvcml6YXRpb24iOiAnQmVhcmVyICcgJiBjb250ZXh0Ll9fYWNjZXNzS2V5CiAgfQp9"
  }
}
```

## Installation

```shell
mvn clean install
```
