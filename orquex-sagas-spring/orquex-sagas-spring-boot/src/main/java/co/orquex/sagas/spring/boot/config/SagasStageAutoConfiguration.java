package co.orquex.sagas.spring.boot.config;

import co.orquex.sagas.spring.framework.config.SagasContextRefreshedListener;
import co.orquex.sagas.spring.framework.config.SagasStageConfiguration;
import co.orquex.sagas.spring.framework.config.SagasTaskConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
  SagasTaskConfiguration.class,
  SagasStageConfiguration.class,
  SagasContextRefreshedListener.class,
  SagasGlobalContextConfiguration.class
})
@ConditionalOnProperty(
    prefix = "orquex.sagas.spring.stage",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SagasStageAutoConfiguration {}
