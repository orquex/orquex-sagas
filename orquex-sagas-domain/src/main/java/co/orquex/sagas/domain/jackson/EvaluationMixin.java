package co.orquex.sagas.domain.jackson;

import co.orquex.sagas.domain.stage.Condition;
import co.orquex.sagas.domain.stage.EvaluationTask;
import co.orquex.sagas.domain.stage.StageConfiguration;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class EvaluationMixin {

  protected EvaluationMixin(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("metadata") Map<String, Serializable> metadata,
      @JsonProperty("configuration") StageConfiguration configuration,
      @JsonProperty("evaluationTask") EvaluationTask evaluationTask,
      @JsonProperty("conditions") List<Condition> conditions,
      @JsonProperty("defaultOutgoing") String defaultOutgoing) {}
}
