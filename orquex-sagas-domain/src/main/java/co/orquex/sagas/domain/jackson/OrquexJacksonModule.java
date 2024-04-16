package co.orquex.sagas.domain.jackson;

import co.orquex.sagas.domain.stage.Activity;
import co.orquex.sagas.domain.stage.Evaluation;
import co.orquex.sagas.domain.stage.Stage;
import co.orquex.sagas.domain.version.OrquexSagasVersion;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.Serial;

/**
 * Jackson {@code Module} for {@code orquex-sagas-domain}, that registers the following mix-in
 * annotations:
 *
 * <ul>
 *   <li>{@link StageMixin}
 * </ul>
 *
 * In other to use this module just add it to your {@code ObjectMapper} configuration.
 *
 * <pre>
 *     ObjectMapper mapper = new ObjectMapper();
 *     mapper.registerModule(new OrquexJacksonModule());
 * </pre>
 */
public class OrquexJacksonModule extends SimpleModule {

  @Serial private static final long serialVersionUID = OrquexSagasVersion.SERIAL_VERSION;

  public OrquexJacksonModule() {
    super(OrquexJacksonModule.class.getName());
  }

  @Override
  public void setupModule(SetupContext context) {
    context.setMixInAnnotations(Stage.class, StageMixin.class);
    context.setMixInAnnotations(Activity.class, ActivityMixin.class);
    context.setMixInAnnotations(Evaluation.class, EvaluationMixin.class);
  }
}
