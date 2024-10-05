package co.orquex.sagas.task.groovy;

import static co.orquex.sagas.task.groovy.TaskConstant.*;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.task.TaskRequest;
import java.io.Serializable;
import java.util.Base64;
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
  public static final String SCRIPT = "__script";

  @Override
  public Map<String, Serializable> execute(TaskRequest taskRequest) {

    try {
      final var metadata = taskRequest.metadata();
      final var payload = taskRequest.payload();

      if (!metadata.containsKey(SCRIPT)) throw new WorkflowException("script not found");

      final var ctx = new HashMap<String, Serializable>();
      final var script = new String(Base64.getDecoder().decode(metadata.get(SCRIPT).toString()));
      final var context =
          new SimpleBindings(Map.of(METADATA, metadata, PAYLOAD, payload, CONTEXT, ctx));
      groovyScriptEngine.eval(script, context);
      return ctx;
    } catch (IllegalArgumentException e) {
      log.error(e.getMessage());
      throw new WorkflowException("Base64 script is invalid");
    } catch (ScriptException e) {
      log.error(e.getMessage());
      throw new WorkflowException(e.getMessage());
    }
  }

  @Override
  public String getKey() {
    return "groovy";
  }
}
