package co.orquex.sagas.spring.framework.config.annotation;

import co.orquex.sagas.spring.framework.config.condition.ConditionOnProperty;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;

/**
 * Custom conditional annotation that checks if a specific property is present and matches a given
 * value.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(ConditionOnProperty.class)
public @interface ConditionalOnProperty {

  /** The name of the property to check. */
  String name();

  /**
   * The expected value of the property. If not specified, the condition will evaluate to true as
   * long as the property is present.
   */
  String havingValue() default "";

  /** Whether the condition should match if the property is missing. */
  boolean matchIfMissing() default false;
}
