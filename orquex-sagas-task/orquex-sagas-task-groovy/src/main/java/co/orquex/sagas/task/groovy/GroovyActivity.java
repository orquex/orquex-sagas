package co.orquex.sagas.task.groovy;

import static co.orquex.sagas.domain.stage.Evaluation.EXPRESSION;
import static co.orquex.sagas.task.groovy.TaskConstant.*;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.exception.WorkflowException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

/** This Groovy implementation class execute scripts. */
@Slf4j
public class GroovyActivity implements TaskImplementation {

  private final GroovyScriptEngineImpl groovyScriptEngine = new GroovyScriptEngineImpl();

  @Override
  public Map<String, Serializable> execute(
      String transactionId, Map<String, Serializable> metadata, Map<String, Serializable> payload) {

    try {
      final var ctx = new HashMap<String, Serializable>();
      final var expression = metadata.get(EXPRESSION).toString();
      final var context =
          new SimpleBindings(Map.of(METADATA, metadata, PAYLOAD, payload, CONTEXT, ctx));
      groovyScriptEngine.eval(expression, context);
      return ctx;
    } catch (ScriptException e) {
      log.error(e.getMessage());
      throw new WorkflowException(e.getMessage());
    }
  }

  @Override
  public String getName() {
    return "groovy";
  }
}
