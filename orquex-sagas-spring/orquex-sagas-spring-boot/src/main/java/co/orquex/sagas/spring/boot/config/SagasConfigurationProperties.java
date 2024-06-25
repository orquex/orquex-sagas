package co.orquex.sagas.spring.boot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "orquex.sagas.spring")
public class SagasConfigurationProperties {

  private WorkflowConfiguration workflow;
  private StageConfiguration stage;

  @Getter
  @Setter
  static class WorkflowConfiguration {
    private boolean enabled;
  }

  @Getter
  @Setter
  static class StageConfiguration {
    private boolean enabled;
  }
}
