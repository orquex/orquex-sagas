# Orquex Sagas Spring

This is a project implementation of the `Orquex Sagas Framework` using `Spring Framework` and `Spring-Boot`.

## Get starter

If you are using `spring-framework` add this maven dependency:

```xml

<dependency>
    <groupId>co.orquex.sagas</groupId>
    <artifactId>orquex-sagas-spring-boot</artifactId>
</dependency>
```

Or with `spring-boot`:

```xml

<dependency>
    <groupId>co.orquex.sagas</groupId>
    <artifactId>orquex-sagas-spring-framework</artifactId>
</dependency>
```

To enable the autoconfiguration add this properties to the `application.yml` or `application.properties`

For enabling workflow autoconfiguration;
```yaml
orquex:
  sagas:
    spring:
      workflow:
        enabled: true
```

And for enabling stage autoconfiguration;
```yaml
orquex:
  sagas:
    spring:
      stage:
        enabled: true
```

Then just inject the `WorkflowExecutor` and call your flows:

```java
private final WorkflowExecutor workflowExecutor;
...
workflowExecutor.execute(request);
```


## Beans automatically injected

This library will inject all default configuration of the `Orquex Sagas framework`:

### Registries

| Interface                      | Implementation                       | Bean name                           |
|--------------------------------|--------------------------------------|-------------------------------------|
| `Registry<TaskImplementation>` | `InMemoryTaskImplementationRegistry` | `defaultTaskImplementationRegistry` |
| `Registry<TaskExecutor>`       | `InMemoryTaskExecutorRegistry`       | `defaultTaskExecutorRegistry`       |
| `Registry<StageExecutor>`      | `InMemoryStageExecutorRegistry`      | `defaultStageExecutorRegistry`      |

### Events

| Interface                     | Implementation                  | Bean name                       |
|-------------------------------|---------------------------------|---------------------------------|
| `EventListener<StageRequest>` | `DefaultStageEventListener`     | `defaultStageEventListener`     |
| `WorkflowEventPublisher`      | `DefaultWorkflowEventPublisher` | `defaultWorkflowEventPublisher` |
| `EventManagerFactory`         | `DefaultEventManagerFactory`    | `defaultEventManagerFactory`    |

### Executors

| Interface                      | Implementation          | Bean name               |
|--------------------------------|-------------------------|-------------------------|
| `Executable<StageRequest>`     | `DefaultStageExecutor`  | `defaultStageExecutor`  |
| `Executable<ExecutionRequest>` | `WorkflowExecutor`      | `workflowExecutor`      |
| `Executable<Checkpoint>`       | `WorkflowStageExecutor` | `workflowStageExecutor` |

### Stage Processing Strategies

| Interface                             | Implementation                 | Bean name |
|---------------------------------------|--------------------------------|-----------|
| `StageProcessingStrategy<Activity>`   | `ActivityProcessingStrategy`   | -         |
| `StageProcessingStrategy<Evaluation>` | `EvaluationProcessingStrategy` | -         |

### Task Implementations

| Interface            | Implementation     | Bean name          |
|----------------------|--------------------|--------------------|
| `TaskImplementation` | `GroovyActivity`   | `groovyActivity`   |
| `TaskImplementation` | `GroovyEvaluation` | `groovyEvaluation` |
