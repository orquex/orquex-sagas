# Sagas Framework

## Prerequisites

- Java 21
- Maven >=3.8.x

## Installation

```shell
mvn clean install
```

## Documentation

Sagas is a known pattern to manage long-live transactions in distributed environments. Long Live Transactions (LLT), is a transaction that takes significant time to be completed and requires synchronization between multiple transactions.

This project aims to define a way to implement this microservices pattern by specifying a structure that solves surrounding problems, developing an implementation of the Sagas pattern with multi-language support and a single communication interface between the different [Flow](#Flow) [Stages](#stage).

## Concepts

### Flow
The flow is the sagas' definition, every flow is unique and contains all the activities and evaluations required to execute it.

### Activity
The activity contains all the tasks that should be executed and manages other features like looping and resilience.

### Task
The task is the atomic unit of the execution flow; it is the adapter between the engine and the external services to share messages, and it is associated with the compensation task.

### Message
Data that is shared between the engine and the services.

### Compensation
This is a task implementation that will be executed during a compensation execution process.

### Evaluators
The evaluators allow control of the flow using inclusive or exclusive conditions.

### Condition
A boolean condition that allows to indicate which path the flow should take.

### Metadata
Message section used for changing the settings or behavior of the sagas for the execution of a Flow.

### Payload
Payload section that contains data shared with the sagas for the execution of a Flow.

## Implementation

For the Saga pattern implementation, there are 2 options, orchestration or choreography, with the orchestration all the execution works in the same machine that received the request, but with choreography, the execution of one transaction is distributed between all deployed engines.

![usecases](./docs/assets/usecases.png)

### Orchestration

![Orchestration](./docs/assets/orchestration.png)

The _Executor_ of the **_Sagas Engine_** should be any system that exposes some interfaces that serve as triggers. 

The **_Sagas Engine_** to start an execution will receive a Flow ID, Metadata, and a Payload, with the ID, can obtain a Flow, which determines the activities and evaluations to be sequentially executed.

Every compensation of an Activity will be added to a stack data structure to be executed in LIFO order.

![sequence-orchestration](./docs/assets/sequence-orchestration.png)

### Choreography

![Choreography](./docs/assets/choreography.png)

In the choreography scenario, the flow execution is distributed using a message broker; once the Sagas Engine obtains the Flow, it will publish the first Step with additional information into the queue, and another instance of the Sagas Engine will be in charge of executing it and putting the next consecutive step in the queue.

> This implementation uses the Saga Engine as a coordinator, to re-use the different Stages in multiple Flows.

![sequence-orchestration](./docs/assets/sequence-choreography-1.png)
![sequence-orchestration](./docs/assets/sequence-choreography-2.png)
![sequence-orchestration](./docs/assets/sequence-choreography-3.png)

### Flow Domain

#### Flow
 
|Attribute|Type|Description|
|--|--|--|
|id|string|The flow identifier (unique).|
|name|string|Name or description of the flow.|
|initial_stage|string|The initial stage of the flow.|
|stages|object|Dictionary or map of Stage.|
|metadata|object|Dictionary or map with additional data.|
|configuration|Configuration|Configuration of the Flow.|

##### Flow Configuration

|Attribute|Type|Description|
|--|--|--|
|timeout|number|The flow identifier (unique).|
|aon|string|All or nothing (true, false). This is in case some of the stages fail, the process will continue if `aon` is false, otherwise, will be stopped.|

#### Stage

|Attribute|Type|Description|
|--|--|--|
|name|string|The stage identifier (unique).|

#### Activity

|Attribute|Type|Description|
|--|--|--|
|task|Task[]|The task that will be executed.|
|metadata|object|Dictionary or map with additional data.|
|sync|boolean|The task’s executions are sync or async,  (default sync).|

#### Task

|Attribute|Type|Description|
|--|--|--|
|id|string|The task identifier (unique).|
|name|string|The task name or description.|
|implementation|string|The implementation type (HTTP, gRPC, Apache Kafka)|
|compensation|Compensation|Compensation definition|
|metadata|object|Dictionary or map with required data.|
|configuration|Configuration|Configuration of the task|

##### Task Compensation

|Attribute|Type|Description|
|--|--|--|
|task|string|Task identifier.|
|metadata|object|Dictionary or map with required data.|

###### Task Configuration

|Attribute|Type|Description|
|--|--|--|
|timeout|number|Execution timeout (ms, sec).|
|resilience|Resilience|The resilience configuration.|

###### Task Resilience Configuration

|Attribute|Type|Description|
|--|--|--|
|timeout|number|Execution timeout (ms, sec).|
|retry|RetryConfiguration|Retry configuration.|
|circuit-breaker|CircuitBreakerConfiguration|Circuit breaker configuration.|

#### Evaluator

|Attribute|Type|Description|
|--|--|--|
|conditions|Condition[]|List of conditions to be evaluated.|
|metadata|object|Dictionary or map with additional data.|
|default|string|Default Stage name to be executed in case of any condition match.|

##### Condition

|Attribute|Type|Description|
|--|--|--|
|expression|string|A boolean expression that depends on the script engine.|
|outgoing|string|A Stage name to be executed in case of a match.|

#### Message

|Attribute|Type|Description|
|--|--|--|
|metadata|object|Dictionary or map with additional data.|
|payload|object|Dictionary or map with task request data.|

### Transaction Domain

#### Transaction State

|Attribute|Type|Description|
|--|--|--|
|transaction_id|string|The transaction identifier (unique).|
|flow_id|string|The flow identifier (unique).|
|data|object|The state data of the transaction.|
|checkpoints|object|A map of a stage name and Checkpoint.|
|status|string|Transaction state type (in_progress, canceled, completed, error)|
|started_at|timestamp|Date time when the transaction is created. |
|updated_at|timestamp|Date time when the transaction is updated.|
|expires_at|timestamp|Date time when the transaction expires.|

#### Checkpoint

|Attribute|Type|Description|
|--|--|--|
|stage|name|The flow identifier (unique).|
|status|string|Checkpoint status (in_progress, canceled, completed, error)|
|request|object|Input of the stage.|
|response|object|Output of the stage.|
