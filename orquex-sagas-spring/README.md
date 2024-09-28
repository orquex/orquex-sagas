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

For disabling the sagas autoconfiguration;

```yaml
orquex:
  sagas:
    spring:
      workflow:
        enabled: false
      stage:
        enabled: false
      event:
        enabled: false
```

### Injection and execution

```java
public class DummyService {

    private final WorkflowExecutor workflowExecutor;
    
    public DummyService(WorkflowExecutor workflowExecutor) {
        this.workflowExecutor = workflowExecutor;
    }
    
    public void execute(ExecutionRequest request) {
        workflowExecutor.execute(request);
    }
}
```

## Beans

This library injects all default configuration of the `Orquex Sagas framework`:

### Registries

| Interface                      | Implementation                       | Bean name                           |
|--------------------------------|--------------------------------------|-------------------------------------|
| `Registry<TaskImplementation>` | `InMemoryTaskImplementationRegistry` | `defaultTaskImplementationRegistry` |
| `Registry<TaskExecutor>`       | `InMemoryTaskExecutorRegistry`       | `defaultTaskExecutorRegistry`       |
| `Registry<StageExecutor>`      | `InMemoryStageExecutorRegistry`      | `defaultStageExecutorRegistry`      |

### Events

| Interface                     | Implementation                          | Bean name                               |
|-------------------------------|-----------------------------------------|-----------------------------------------|
| `EventListener<StageRequest>` | `DefaultStageEventListener`             | `defaultStageEventListener`             |
| `WorkflowEventPublisher`      | `DefaultWorkflowEventPublisher`         | `defaultWorkflowEventPublisher`         |
| `EventManagerFactory`         | `DefaultEventManagerFactory`            | `defaultEventManagerFactory`            |
| `EventListener<Checkpoint>`   | `DefaultCheckpointEventListener`        | `defaultCheckpointEventListener`        |
| -                             | `DefaultCheckpointEventListenerHandler` | `defaultCheckpointEventListenerHandler` |

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
