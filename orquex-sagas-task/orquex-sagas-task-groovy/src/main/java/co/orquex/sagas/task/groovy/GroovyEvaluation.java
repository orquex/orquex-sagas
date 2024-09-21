package co.orquex.sagas.task.groovy;

import static co.orquex.sagas.task.groovy.TaskConstant.METADATA;
import static co.orquex.sagas.task.groovy.TaskConstant.PAYLOAD;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.exception.WorkflowException;
import java.io.Serializable;
import java.util.Map;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

/** This Groovy implementation class, evaluates a boolean expression. */
@Slf4j
public final class GroovyEvaluation implements TaskImplementation {

  private final GroovyScriptEngineImpl groovyScriptEngine = new GroovyScriptEngineImpl();
  public static final String EXPRESSION = "__expression";
  public static final String RESULT = "__result";

  @Override
  public Map<String, Serializable> execute(
      final String transactionId,
      final Map<String, Serializable> metadata,
      final Map<String, Serializable> payload) {

    try {
      if (!metadata.containsKey(EXPRESSION)) throw new WorkflowException("expression not found");

      final var expression = metadata.get(EXPRESSION).toString();
      final var context = new SimpleBindings(Map.of(METADATA, metadata, PAYLOAD, payload));
      if (groovyScriptEngine.eval(expression, context) instanceof Boolean result)
        return Map.of(RESULT, result);
      throw new WorkflowException("expression is not boolean");
    } catch (ScriptException e) {
      log.error(e.getMessage());
      throw new WorkflowException(e.getMessage());
    }
  }

  @Override
  public String getKey() {
    return "groovy-eval";
  }
}
