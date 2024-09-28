package co.orquex.sagas.spring.boot.config;

import co.orquex.sagas.spring.framework.config.SagasWorkflowConfiguration;
import co.orquex.sagas.spring.framework.config.event.SagasEventListenerConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@AutoConfigureAfter(SagasWorkflowConfiguration.class)
@Import({SagasEventListenerConfiguration.class})
@ConditionalOnProperty(
    prefix = "orquex.sagas.spring.event",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SagasEventListenerAutoConfiguration {}
