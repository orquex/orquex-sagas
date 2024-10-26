package co.orquex.sagas.spring.boot.config;

import co.orquex.sagas.spring.framework.config.SagasAsyncWorkflowConfiguration;
import co.orquex.sagas.spring.framework.config.SagasWorkflowConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@AutoConfigureAfter({SagasAsyncWorkflowConfiguration.class, SagasWorkflowConfiguration.class})
@Import({SagasAsyncWorkflowConfiguration.class, SagasWorkflowConfiguration.class})
@ConditionalOnProperty(
    prefix = "orquex.sagas.spring.workflow",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SagasWorkflowAutoConfiguration {}
