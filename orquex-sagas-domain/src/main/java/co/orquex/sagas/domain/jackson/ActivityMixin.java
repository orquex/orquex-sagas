package co.orquex.sagas.domain.jackson;

import co.orquex.sagas.domain.stage.ActivityTask;
import co.orquex.sagas.domain.stage.StageConfiguration;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class ActivityMixin {

  public ActivityMixin(
          @JsonProperty("id") String id,
          @JsonProperty("name") String name,
          @JsonProperty("metadata") Map<String, Serializable> metadata,
          @JsonProperty("configuration") StageConfiguration configuration,
          @JsonProperty("activityTasks") List<ActivityTask> tasks,
          @JsonProperty("sync") boolean sync,
          @JsonProperty("outgoing") String outgoing) {}
}
