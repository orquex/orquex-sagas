package co.orquex.sagas.spring.framework.config.annotation;

import co.orquex.sagas.spring.framework.config.condition.ConditionOnMissingBean;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;

/** Conditional annotation to check if a bean is missing. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(value = ConditionOnMissingBean.class)
public @interface ConditionalOnMissingBean {

  /** The name of the bean to check if it's missing. */
  String[] name() default {};
}
