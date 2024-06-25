package co.orquex.sagas.spring.boot.config;

import co.orquex.sagas.spring.framework.config.SagasWorkflowConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@AutoConfiguration
@Import({SagasWorkflowConfiguration.class})
@ConditionalOnProperty(
    prefix = "orquex.sagas.spring.workflow",
    name = "enabled",
    havingValue = "true")
@PropertySource("classpath:orquex-sagas-spring.properties")
public class SagasWorkflowAutoConfiguration {}
