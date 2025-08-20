# OrqueX SAGA Framework

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/orquex/orquex-sagas)

## 🚀 Build Bulletproof Distributed Workflows

OrqueX SAGA is a modern, developer-friendly framework for building resilient distributed workflows that just work.
Whether you're processing payments, managing orders, or orchestrating complex business processes across microservices,
SAGA handles the hard parts so you can focus on your business logic.

### Why SAGA?

**Distributed systems are hard.** When your workflow spans multiple services, databases, or APIs, things can (and will)
go wrong:

- Services crash mid-transaction
- Network calls timeout
- Partial failures leave your system in inconsistent states
- Rolling back distributed changes becomes a nightmare

**SAGA solves these problems** by providing:

- ✅ **Automatic compensation** - Failed operations are automatically rolled back
- ✅ **Resume from failure** - Workflows continue exactly where they left off after crashes
- ✅ **Visual workflow definition** - Define complex flows with simple JSON/YAML
- ✅ **Built-in resilience** - Retries, circuit breakers, and timeouts out of the box
- ✅ **Zero learning curve** - Works with your existing Java/Spring applications

### Real-World Example

```json
{
  "id": "order-processing",
  "stages": {
    "payment": {
      "id": "payment",
      "type": "activity",
      "tasks": [
        {
          "task": "charge-credit-card"
        }
      ],
      "outgoing": "inventory"
    },
    "inventory": {
      "id": "inventory",
      "type": "activity",
      "tasks": [
        {
          "task": "reserve-items"
        }
      ],
      "outgoing": "shipping"
    },
    "shipping": {
      "id": "shipping",
      "type": "activity",
      "tasks": [
        {
          "task": "create-shipment"
        }
      ]
    }
  }
}
```

If payment succeeds but inventory fails, SAGA automatically:

1. 🔄 Refunds the credit card charge
2. 📊 Logs the failure with full context
3. 🎯 Allows you to retry from the inventory step

### What Makes OrqueX Different?

OrqueX SAGA is built for **real production workloads** with enterprise-grade features:

- **Production-Ready**: Battle-tested resilience patterns (retry, circuit breaker, timeouts)
- **Developer Experience**: Rich Spring Boot integration, clear error messages, debugging tools
- **Flexible Integration**: Works with REST APIs, databases, message queues, or custom logic
- **Preprocessing & Postprocessing**: Transform data between services without changing existing APIs
- **Extensible**: Plugin architecture for custom tasks and integrations

This framework extends the traditional SAGA pattern with practical features that solve real integration challenges in
distributed systems.

## Prerequisites

- Java 21
- Maven >=3.8.x

## Installation

```shell
mvn clean install
```

## Implementation Examples

### Spring Boot

- [Basic Sample](./orquex-sagas-spring/samples/basic-sample)
- [Coffee Shop](./orquex-sagas-spring/samples/coffee-shop)

## Built-in tasks integrations

- [orquex-sagas-task-groovy](./orquex-sagas-task/orquex-sagas-task-groovy)
- [orquex-sagas-task-http-api](./orquex-sagas-task/orquex-sagas-task-http/orquex-sagas-task-http-api)
- [orquex-sagas-task-okhttp](./orquex-sagas-task/orquex-sagas-task-http/orquex-sagas-task-okhttp)
- [orquex-sagas-task-jsonata](orquex-sagas-task/orquex-sagas-task-jsonata)

---

## Domain

### Flow

The flow is the sagas' definition; every flow is unique and contains all the stages required to execute it.

| Attribute     | Type   | Required | Description                              |
|---------------|--------|----------|------------------------------------------|
| id            | string | true     | A unique identifier for the flow.        |
| name          | string | false    | A human-readable name for the flow.      |
| initial_stage | string | true     | The initial stage of the flow.           |
| stages        | object | true     | Dictionary or map of [Stage](#stage).    |
| metadata      | object | false    | Dictionary or map with additional data.  |
| configuration | object | false    | [FlowConfiguration](#flow-configuration) |

#### Flow Configuration

The flow configuration contains the flow timeout, all-or-nothing flag, and resume-from-failure capability required to
handle the flow behaviour.

| Attribute         | Type    | Required | Description                                                                                                         |
|-------------------|---------|----------|---------------------------------------------------------------------------------------------------------------------|
| timeout           | string  | false    | A timeout of the flow. (default: 1 minute)                                                                          |
| all_or_nothing    | boolean | false    | All or nothing (true, false). If false, process continues on stage failure.                                         |
| resumeFromFailure | boolean | false    | Resume from failure (true, false). If true, enables checkpoint-based recovery when failures occur. (default: false) |

### Stage

The stage is the main unit of work in the flow; every stage is unique and contains all the information required to
execute it. This is the base class for Activity and Evaluation stages.

| Attribute     | Type   | Required | Description                                                       |
|---------------|--------|----------|-------------------------------------------------------------------|
| id            | string | false    | A unique identifier for the Stage. (default: auto-generated UUID) |
| name          | string | false    | A human-readable name for the Stage. (default: stage ID)          |
| type          | string | true     | Specifies the type of the stage (`activity`, `evaluation`).       |
| metadata      | object | false    | Dictionary or map with additional data.                           |
| configuration | object | false    | [StageConfiguration](#stage-configuration)                        |   

#### Stage Configuration

The stage configuration contains the implementation and parameters required to execute the stage.

| Attribute      | Type   | Required | Description                                                 |
|----------------|--------|----------|-------------------------------------------------------------|
| implementation | string | false    | The implementation type of the stage. (default: 'default')  |
| parameters     | object | false    | Dictionary or map with additional information to the stage. |

### Activity

The Activity is a specialized type of Stage that contains all the tasks that should be executed and manages other
features like looping and resilience.

| Attribute     | Type    | Required | Description                                                                                   |
|---------------|---------|----------|-----------------------------------------------------------------------------------------------|
| activityTasks | array   | true     | List of [ActivityTask](#activitytask) to be executed.                                         |
| parallel      | boolean | false    | Whether tasks should be executed in parallel (true) or sequentially (false). (default: false) |
| outgoing      | string  | false    | The next stage identifier to be executed.                                                     |
| allOrNothing  | boolean | false    | All or nothing behavior for the activity. (default: true)                                     |

#### ActivityTask

The activity task contains the information necessary to pre-process the input request of a task, then execute it and
finally post-process its response, while recording its compensation.

| Attribute      | Type   | Required | Description                                                                                     |
|----------------|--------|----------|-------------------------------------------------------------------------------------------------|
| id             | string | false    | A unique identifier for the activity task. (default: auto-generated UUID)                       |
| name           | string | false    | A human-readable name for the activity task. (default: task identifier)                         |
| task           | string | true     | The task identifier.                                                                            |
| pre_processor  | object | false    | Task processor executed before the main task. [TaskProcessor](#taskprocessor)                   |
| post_processor | object | false    | Task processor executed after the main task. [TaskProcessor](#taskprocessor)                    |
| compensation   | object | false    | Compensation task executed if main/post-processing fails. [CompensationTask](#compensationtask) |
| metadata       | object | false    | Dictionary or map with additional data for the task.                                            |

#### TaskProcessor

The task processor encapsulates the identifier of a task and its additional parameters to be executed from an activity.

| Attribute | Type   | Required | Description                                          |
|-----------|--------|----------|------------------------------------------------------|
| task      | string | true     | The task identifier to be processed.                 |
| metadata  | object | false    | Dictionary or map with additional data for the task. |

### Evaluation

The Evaluation is a specialized type of Stage that contains logic to determine the next stage to execute based on
certain conditions. It includes an EvaluationTask that encapsulates the task for evaluating the conditions, a list of
Condition objects that
define the conditions to be evaluated, and a defaultOutgoing string that specifies the ID of the default stage to
transition to if none of the conditions are met.

| Attribute       | Type   | Required | Description                                                      |
|-----------------|--------|----------|------------------------------------------------------------------|
| evaluationTask  | object | true     | [EvaluationTask](#evaluationtask) for evaluating the conditions. |
| conditions      | array  | true     | List of [Condition](#condition) to be evaluated.                 |
| defaultOutgoing | string | true     | Default Stage name to be executed if no condition matches.       |

#### EvaluationTask

Represents an evaluation task within an Evaluation stage in a workflow. An EvaluationTask contains the task information
for evaluating conditions, with optional pre-processing and metadata configuration.

| Attribute     | Type   | Required | Description                                                                   |
|---------------|--------|----------|-------------------------------------------------------------------------------|
| id            | string | false    | A unique identifier for the evaluation task. (default: auto-generated UUID)   |
| name          | string | false    | A human-readable name for the evaluation task. (default: task identifier)     |
| task          | string | true     | The task identifier to execute the evaluations.                               |
| pre_processor | object | false    | Task processor executed before the main task. [TaskProcessor](#taskprocessor) |
| metadata      | object | false    | Dictionary or map with additional data.                                       |

#### Condition

The Condition is evaluated during the execution of an EvaluationTask; it contains an expression that defines the
outgoing string that specifies the ID of the stage to transition to if the condition is met.

| Attribute  | Type   | Required | Description                                                   |
|------------|--------|----------|---------------------------------------------------------------|
| expression | string | true     | A boolean expression that depends on the script engine.       |
| outgoing   | string | true     | The ID of the stage to transition to if the condition is met. |

### Task

This is used to define and manage the details of a specific task in a workflow.

| Attribute      | Type   | Required | Description                             |
|----------------|--------|----------|-----------------------------------------|
| id             | string | true     | The task identifier.                    |
| name           | string | false    | A human-readable name for the task.     |
| implementation | string | true     | The implementation type of the task.    |
| compensation   | object | false    | [TaskProcessor](#taskprocessor)         |
| metadata       | object | false    | Dictionary or map with additional data. |
| configuration  | object | false    | [TaskConfiguration](#taskconfiguration) |

#### TaskConfiguration

Defines and manages the configuration and behaviour of a task.

| Attribute  | Type   | Required | Description                                                              |
|------------|--------|----------|--------------------------------------------------------------------------|
| executor   | string | true     | The executor that will execute the task.                                 |
| resilience | object | false    | [ResilienceConfiguration](#resilienceconfiguration)                      |
| parameters | object | false    | Dictionary or map with additional information to the task configuration. |

##### ResilienceConfiguration

Represents the configuration for resilience in a task. It includes timeout, retry and circuit breaker configurations.

| Attribute       | Type   | Required | Description                                                 |
|-----------------|--------|----------|-------------------------------------------------------------|
| timeout         | string | false    | The timeout of the task. (default: 1 minute)                |
| retry           | object | false    | [RetryConfiguration](#retryconfiguration)                   |
| circuit_breaker | object | false    | [CircuitBreakerConfiguration](#circuitbreakerconfiguration) |

###### RetryConfiguration

Defines retry behavior parameters for handling transient failures with configurable attempts and delays.

| Attribute              | Type    | Required | Description                                                                                            |
|------------------------|---------|----------|--------------------------------------------------------------------------------------------------------|
| maxAttempts            | number  | true     | Maximum number of execution attempts (including initial attempt). Must be positive.                    |
| waitDuration           | string  | true     | Duration to wait between retry attempts (ISO-8601 format, e.g., "PT1S"). Must be positive.             |
| retryWorkflowException | boolean | false    | Whether to retry when WorkflowException is thrown. Default: false.                                     |
| successPolicyTask      | object  | false    | Optional task processor that validates if a result represents success. [TaskProcessor](#taskprocessor) |

###### CircuitBreakerConfiguration

Defines circuit breaker behavior parameters for protecting against cascading failures with configurable thresholds and
recovery.

| Attribute               | Type   | Required | Description                                                                                            |
|-------------------------|--------|----------|--------------------------------------------------------------------------------------------------------|
| failureThreshold        | number | true     | Number of consecutive failures to transition from CLOSED to OPEN state. Must be positive.              |
| waitDurationInOpenState | string | true     | Duration to remain in OPEN state before transitioning to HALF_OPEN (ISO-8601 format, e.g., "PT1M").    |
| successThreshold        | number | true     | Number of consecutive successes in HALF_OPEN state to transition back to CLOSED. Must be positive.     |
| successPolicyTask       | object | false    | Optional task processor that validates if a result represents success. [TaskProcessor](#taskprocessor) |
| fallbackTask            | object | false    | Optional fallback task processor executed when circuit is open. [TaskProcessor](#taskprocessor)        |

### Flow States

#### Transaction

The Transaction encapsulates the status of a flow, including its unique identifiers, data, and timestamps.

| Attribute      | Type      | Description                                                      |
|----------------|-----------|------------------------------------------------------------------|
| transaction_id | string    | The transaction identifier (unique).                             |
| flow_id        | string    | The flow identifier (unique).                                    |
| correlation_id | string    | The correlation identifier (unique).                             |
| data           | object    | The state data of the transaction.                               |
| status         | string    | Transaction state type (in_progress, canceled, completed, error) |
| started_at     | timestamp | Date time when the transaction is created.                       |
| updated_at     | timestamp | Date time when the transaction is updated.                       |
| expires_at     | timestamp | Date time when the transaction expires.                          |

#### Checkpoint

Represents a checkpoint in a workflow transaction.

A Checkpoint is created every time a stage is executed in a workflow.
It encapsulates the status of the execution, including the transaction and flow identifiers, correlation identifier,
metadata, request and response data, and timestamps.
It also includes the outgoing stage identifier and the incoming stage.

Each execution of a stage will generate multiple checkpoints with different statuses, allowing for tracking and auditing
of the workflow.

| Attribute     | Type      | Description                                                                              |
|---------------|-----------|------------------------------------------------------------------------------------------|
| transactionId | string    | The transaction identifier (unique).                                                     |
| status        | string    | The checkpoint [Status](#status) (in_progress, canceled, completed, error) of the stage. |
| flowId        | string    | The flow identifier (unique).                                                            |
| correlationId | string    | The correlation identifier (unique) of the flow.                                         |
| stageId       | string    | The stage identifier that was executed.                                                  |
| metadata      | object    | The metadata of the executed stage.                                                      |
| payload       | object    | The payload data processed by the stage.                                                 |
| response      | object    | The response data returned by the stage.                                                 |
| outgoing      | string    | The outgoing stage identifier.                                                           |
| incoming      | object    | The incoming serialized [Stage](#stage).                                                 |
| createdAt     | timestamp | Date time when the checkpoint is created.                                                |
| updatedAt     | timestamp | Date time when the checkpoint is updated.                                                |

#### Status

General status used during the execution of a flow.

| Status      | Description                  |
|-------------|------------------------------|
| IN_PROGRESS | Indicate that is executing.  |
| CANCELED    | Indicate that was canceled.  |
| COMPLETED   | Indicate that was completed. |
| ERROR       | Indicate that was an error.  |

#### Compensation

Represents a compensation event message in a workflow transaction.

A Compensation is generated every time an activity task is executed, and it contains a compensation task processor. It
encapsulates the transaction identifier, task name, metadata, request and response data, and the timestamp when it
was created.

This is sent via an event for further processing.

| Attribute      | Type      | Description                                 |
|----------------|-----------|---------------------------------------------|
| transaction_id | string    | The transaction identifier (unique).        |
| task           | string    | The task identifier.                        |
| metadata       | object    | The metadata of the task.                   |
| request        | object    | The request of the task.                    |
| response       | object    | The response of the task.                   |
| created_at     | timestamp | Date time when the compensation is created. |

#### CompensationTask

The CompensationTask represents a task that is executed to compensate (undo or rollback) the effects of a previously executed task
when a workflow fails. It contains the necessary information to reverse or rollback operations performed by the main
task.

| Attribute     | Type   | Required | Description                                                                                         |
|---------------|--------|----------|-----------------------------------------------------------------------------------------------------|
| task          | string | true     | The task identifier for the compensation operation.                                                 |
| preProcessor  | object | false    | Optional task processor executed before the main compensation task. [TaskProcessor](#taskprocessor) |
| postProcessor | object | false    | Optional task processor executed after the main compensation task. [TaskProcessor](#taskprocessor)  |
| metadata      | object | false    | Dictionary or map with additional data for the compensation task.                                   |

## Metadata and Payload

### Metadata

The metadata is a dictionary or map that contains additional information for a flow, stage, or task, and is shared
throughout the execution stack. Metadata provides contextual information that can influence execution behavior,
configuration, and decision-making during workflow processing.

#### Metadata Flow

Metadata flows through the execution hierarchy and gets merged at each level:

```
Execution Request → Flow → Stage → Activity Task → Task
```

#### Metadata Merging Strategy

During execution, metadata is merged using the following priority (highest to lowest):

1. **Stage Metadata** - Specific to the current stage being executed
2. **Flow Metadata** - Global metadata defined at the flow level
3. **Request Metadata** - Initial metadata provided in the execution request

#### Common Metadata Use Cases

- **Configuration Parameters**: Dynamic configuration values for tasks
- **Security Context**: Authentication tokens, user permissions, tenant information
- **Tracing Information**: Correlation IDs, trace spans, debugging flags
- **Business Context**: Customer information, environment settings, feature flags
- **Execution Hints**: Retry policies, timeout overrides, priority levels

### Payload

The payload represents the actual business data being processed through the workflow. Unlike metadata (which provides
context), payload contains the core information that stages operate on and transform during execution.

#### Payload Flow

The payload flows sequentially through the workflow stages in a chain-like manner. Each stage receives the payload from
the previous stage, processes it, and produces an output payload that becomes the input for the next stage. Payloads are
**not shared** between stages; instead, they are **transformed** and **passed forward**:

```
Initial Payload → Stage 1 (process) → Output Payload A → Stage 2 (process) → Output Payload B → Stage 3 → Final Result
```

**Key Characteristics:**

- **Sequential Processing**: Each stage operates on the output of the previous stage
- **Transformation Chain**: The response/output of one stage becomes the request/input of the next
- **Isolated Processing**: Stages don't have access to the original payload or outputs from non-adjacent stages
- **Linear Flow**: Data flows in one direction through the workflow pipeline

#### Payload Transformation

- Each stage can **read** the incoming payload
- Each stage can **modify** or **transform** the payload
- The output payload becomes the input for the next stage
- The final payload is returned as the workflow result

#### Payload vs Metadata

| Aspect             | Payload                          | Metadata                                 |
|--------------------|----------------------------------|------------------------------------------|
| **Purpose**        | Business data being processed    | Contextual information and configuration |
| **Transformation** | Modified by each stage           | Merged and inherited                     |
| **Scope**          | Core workflow data               | Supporting information                   |
| **Usage**          | Primary input/output of stages   | Configuration and context                |
| **Persistence**    | Stored in checkpoints for resume | Stored in checkpoints for resume         |

#### Payload in Checkpoints

When resume-from-failure is enabled, both payload and metadata are stored in checkpoints:

- **Current payload state** is preserved for recovery
- **Metadata context** is maintained across restarts
- **Stage-specific transformations** can be resumed from the exact point of failure

#### Best Practices

**Payload:**

- Keep payload focused on business data
- Ensure payload is serializable for checkpoint storage
- Document expected payload structure for each stage
- Validate payload structure at stage boundaries

**Metadata:**

- Use metadata for configuration and context, not business data
- Prefer standard metadata keys across workflows
- Keep metadata lightweight and focused
- Document metadata requirements for reusable stages

---

## License

This project is licensed under the terms of the [MIT License](LICENSE).
