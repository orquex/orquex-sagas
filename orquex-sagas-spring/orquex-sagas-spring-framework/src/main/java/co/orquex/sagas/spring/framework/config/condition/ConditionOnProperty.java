package co.orquex.sagas.spring.framework.config.condition;

import co.orquex.sagas.spring.framework.config.annotation.ConditionalOnProperty;
import java.util.Optional;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

/** Condition implementation for {@link ConditionalOnProperty}. */
public class ConditionOnProperty implements Condition {

  @Override
  public boolean matches(@NonNull ConditionContext context, AnnotatedTypeMetadata metadata) {
    // Retrieve annotation attributes
    final var attributes = metadata.getAnnotationAttributes(ConditionalOnProperty.class.getName());
    if (attributes == null) return false;

    final var name = (String) attributes.get("name");
    final var havingValue = (String) attributes.get("havingValue");
    boolean matchIfMissing = (Boolean) attributes.get("matchIfMissing");

    final var environment = context.getEnvironment();

    // Check if the property is present in the environment
    final var propertyValue = Optional.ofNullable(environment.getProperty(name));

    return propertyValue
        .map(s -> havingValue.isEmpty() || havingValue.equals(s))
        .orElse(matchIfMissing);
  }
}
