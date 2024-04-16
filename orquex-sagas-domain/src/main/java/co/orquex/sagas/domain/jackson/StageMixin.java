package co.orquex.sagas.domain.jackson;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXISTING_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import co.orquex.sagas.domain.stage.Activity;
import co.orquex.sagas.domain.stage.Evaluation;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, include = EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = Activity.class, name = "activity"),
  @JsonSubTypes.Type(value = Evaluation.class, name = "evaluation")
})
abstract class StageMixin {}
