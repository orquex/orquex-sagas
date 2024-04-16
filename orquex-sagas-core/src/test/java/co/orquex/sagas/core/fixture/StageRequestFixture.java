package co.orquex.sagas.core.fixture;

import co.orquex.sagas.domain.execution.ExecutionRequest;
import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.stage.StageRequest;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StageRequestFixture {

  public static StageRequest getStageRequest(Stage stage, ExecutionRequest executionRequest) {
    return StageRequest.builder()
        .transactionId(UUID.randomUUID().toString())
        .stage(stage)
        .executionRequest(executionRequest)
        .build();
  }
}
