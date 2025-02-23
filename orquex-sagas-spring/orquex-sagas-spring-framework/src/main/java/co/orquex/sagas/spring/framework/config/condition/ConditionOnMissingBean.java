package co.orquex.sagas.spring.framework.config.condition;

import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnMissingBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

/** Condition to check if a bean is missing. */
@Slf4j
public class ConditionOnMissingBean implements Condition {

  @Override
  public boolean matches(@NonNull ConditionContext context, AnnotatedTypeMetadata metadata) {
    final var attributes =
        metadata.getAnnotationAttributes(ConditionalOnMissingBean.class.getName());

    if (attributes == null) return false;
    final var value = attributes.get("name");

    if (value instanceof String[] names) {
      for (String name : names) {
        if (context.getRegistry().containsBeanDefinition(name)) {
          log.debug("Skipping default bean definition for '{}'", name);
          return false;
        }
      }
    }
    return true;
  }
}
