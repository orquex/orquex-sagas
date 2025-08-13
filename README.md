# SAGA Framework

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/orquex/orquex-sagas)

SAGA (Software Automation, Generation, and Administration) is a known pattern to manage long-live transactions in
distributed environments.
Long Live Transactions (LLT) are transactions that take significant time to complete and require synchronization between
multiple transactions.

When working in a distributed system, it is often necessary to maintain transactionality during the execution of a
defined process, maintaining the properties of an ACID transaction (Atomicity, Consistency, Isolation, and Durability)
to ensure that processed and generated data are valid. However, the Saga pattern does not have the property of
isolation, which can generate interference between different processes or transactions. A solution to this problem was
proposed
by [Daraghmi, E. et al. (2022)](https://www.proquest.com/scholarly-journals/enhancing-saga-pattern-distributed-transactions/docview/2679673542/se-2)
with the quota cache mechanism and commit-sync service.

This project aims to define a way to implement this microservices pattern by specifying a structure that solves
surrounding problems during the implementation and integration of a distributed system using this pattern.
The problems solved in this project are: definition of a domain for the creation of a flow, definition of the
implementation and integration interfaces, and in addition to the main behaviour of the pattern (its compensation), two
new mechanisms are integrated: preprocessing and post-processing, which solve the integration problem when contracts or
communication interfaces are unknown, thus allowing extension to any type of implementation.

The implementation is currently developed in Java, and is expected to be extended to other programming languages in the
future.

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

## Tasks integrations

- [orquex-sagas-task-groovy](./orquex-sagas-task/orquex-sagas-task-groovy)
- [orquex-sagas-task-http-api](./orquex-sagas-task/orquex-sagas-task-http/orquex-sagas-task-http-api)
- [orquex-sagas-task-okhttp](./orquex-sagas-task/orquex-sagas-task-http/orquex-sagas-task-okhttp)
- [orquex-sagas-task-jsonata](orquex-sagas-task/orquex-sagas-task-jsonata)

## Documentation

For the Saga pattern implementation, there are two options: orchestration or choreography.
With orchestration, all execution works in the same machine that received the request.
With choreography, the execution of one transaction is distributed between all deployed engines.

![usecases](./docs/assets/usecases.png)

### Orchestration

![Orchestration](./docs/assets/orchestration.png)

The _Executor_ of the **Sagas Engine** should be any system that exposes some interfaces that serve as triggers.

The **Sagas Engine** to start an execution will receive a Flow ID, Metadata, and a Payload. With the ID, it can obtain a
Flow, which determines the activities and evaluations to be sequentially executed.

Every compensation of an Activity will be added to a stack data structure to be executed in LIFO order.

![sequence-orchestration](./docs/assets/sequence-orchestration.png)

### Choreography

![Choreography](./docs/assets/choreography.png)

In the choreography scenario, the flow execution is distributed using a message broker. Once the Sagas Engine obtains
the Flow, it will publish the first Step with additional information into the queue, and another instance of the Sagas
Engine will be in charge of executing it and putting the next consecutive step in the queue.

> This implementation uses the Saga Engine as a coordinator, to re-use the different Stages in multiple Flows.

![sequence-choreography-1](./docs/assets/sequence-choreography-1.png)
![sequence-choreography-2](./docs/assets/sequence-choreography-2.png)
![sequence-choreography-3](./docs/assets/sequence-choreography-3.png)

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

The flow configuration contains the flow timeout and the all-or-nothing flag required to handle the flow behaviour.

| Attribute      | Type    | Required | Description                                                                 |
|----------------|---------|----------|-----------------------------------------------------------------------------|
| timeout        | string  | false    | A timeout of the flow. (default: 1 minute)                                  |
| all_or_nothing | boolean | false    | All or nothing (true, false). If false, process continues on stage failure. |

### Stage

The stage is the main unit of work in the flow; every stage is unique and contains all the information required to
execute it.

| Attribute     | Type    | Required | Description                                                 |
|---------------|---------|----------|-------------------------------------------------------------|
| id            | string  | true     | A unique identifier for the Stage.                          |
| name          | string  | false    | A human-readable name for the Stage.                        |
| type          | string  | true     | Specifies the type of the stage (`activity`, `evaluation`). |
| metadata      | object  | false    | Dictionary or map with additional data.                     |
| configuration | object  | false    | [StageConfiguration](#stage-configuration)                  |
| outgoing      | string  | false    | The next stage identifier to be executed.                   |
| allOrNothing  | boolean | false    | The next stage identifier to be executed.                   |

#### Stage Configuration

The stage configuration contains the implementation and parameters required to execute the stage.

| Attribute      | Type   | Required | Description                                                 |
|----------------|--------|----------|-------------------------------------------------------------|
| implementation | string | false    | The implementation type of the stage. (default: 'default')  |
| parameters     | object | false    | Dictionary or map with additional information to the stage. |

### Activity

The activity contains all the tasks that should be executed and manages other features like looping and resilience.

#### ActivityTask

The activity task contains the information necessary to pre-process the input request of a task, then execute it and
finally post-process its response, while recording its compensation.

| Attribute      | Type   | Required | Description                                                                               |
|----------------|--------|----------|-------------------------------------------------------------------------------------------|
| task           | string | true     | The task identifier.                                                                      |
| pre_processor  | object | false    | Task processor executed before the main task. [TaskProcessor](#taskprocessor)             |
| post_processor | object | false    | Task processor executed after the main task. [TaskProcessor](#taskprocessor)              |
| compensation   | object | false    | Compensation task executed if main/post-processing fails. [TaskProcessor](#taskprocessor) |
| metadata       | object | false    | Dictionary or map with additional data for the task.                                      |

#### TaskProcessor

The task processor encapsulates the identifier of a task and its additional parameters to be executed from an activity.

| Attribute | Type   | Required | Description                                          |
|-----------|--------|----------|------------------------------------------------------|
| task      | string | true     | The task identifier to be processed.                 |
| metadata  | object | false    | Dictionary or map with additional data for the task. |

### Evaluation

The Evaluation is a specialized type of Stage that contains logic to determine the next stage to execute based on
certain conditions.
It includes an EvaluationTask that encapsulates the task for evaluating the conditions, a list of Condition objects that
define the conditions to be evaluated, and a defaultOutgoing string that specifies the ID of the default stage to
transition to if none of the conditions are met.

| Attribute       | Type   | Required | Description                                                      |
|-----------------|--------|----------|------------------------------------------------------------------|
| evaluation_task | object | true     | [EvaluationTask](#evaluationtask) for evaluating the conditions. |
| conditions      | array  | true     | List of [Condition](#condition) to be evaluated.                 |
| default         | string | true     | Default Stage name to be executed if no condition matches.       |

#### EvaluationTask

Data that is shared between the engine and the services.

| Attribute     | Type   | Required | Description                                                                   |
|---------------|--------|----------|-------------------------------------------------------------------------------|
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

TODO

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|

###### CircuitBreakerConfiguration

TODO

| Attribute | Type | Required | Description |
|-----------|------|----------|-------------|

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

| Attribute      | Type      | Description                                                                                       |
|----------------|-----------|---------------------------------------------------------------------------------------------------|
| transaction_id | string    | The transaction identifier (unique).                                                              |
| flow_id        | string    | The flow identifier (unique).                                                                     |
| correlation_id | string    | The correlation identifier (unique) of the flow.                                                  |
| incoming       | object    | The incoming serialized [Stage](#stage).                                                          |
| outgoing       | string    | The outgoing stage identifier.                                                                    |
| metadata       | object    | The metadata of the incoming stage.                                                               |
| request        | object    | The request of the incoming stage.                                                                |
| response       | object    | The response of the incoming stage.                                                               |
| status         | string    | The checkpoint [Status](#status) (in_progress, canceled, completed, error) of the incoming stage. |
| started_at     | timestamp | Date time when the checkpoint is created.                                                         |
| updated_at     | timestamp | Date time when the checkpoint is updated.                                                         |
| expires_at     | timestamp | Date time when the checkpoint expires.                                                            |

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

A Compensation is generated every time an activity task is executed, and it contains a compensation task processor.
It encapsulates the transaction identifier, task name, metadata, request and response data, and the timestamp when it
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

## Metadata

The metadata is a dictionary or map that contains additional information for a flow, stage, task, or checkpoint, and is
shared between the stack execution.

With the first workflow execution, it will start from `Execution Request` to the `Task` e.g.
`Execution Request → Flow → Stage → Activity Task → Task`, and the subsequent workflow execution will start from the
`Flow` to the `Task` e.g. `Flow → Stage → Activity Task → Task`.

---

## License

This project is licensed under the terms of the [MIT License](LICENSE).
