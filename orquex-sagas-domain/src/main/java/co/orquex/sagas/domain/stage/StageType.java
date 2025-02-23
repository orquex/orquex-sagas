package co.orquex.sagas.domain.stage;

/**
 * This enumeration defines types of stages that determines the behavior of the stage.
 *
 * <ul>
 *   <li>`activity`: Represents a stage that performs a specific task or action.
 *   <li>`evaluation`: Represents a stage that evaluates conditions to determine the next stage to
 *       execute.
 * </ul>
 *
 * @see Activity
 * @see Evaluation
 */
public enum StageType {
  activity,
  evaluation
}
