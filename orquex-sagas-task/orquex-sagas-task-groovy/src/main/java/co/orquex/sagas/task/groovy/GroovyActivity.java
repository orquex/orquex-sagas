package co.orquex.sagas.task.groovy;

import static co.orquex.sagas.task.groovy.TaskConstant.*;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.api.context.GlobalContext;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.task.TaskRequest;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

/** This Groovy implementation class execute scripts. */
@Slf4j
@RequiredArgsConstructor
public class GroovyActivity implements TaskImplementation {

  private final GroovyScriptEngineImpl groovyScriptEngine = new GroovyScriptEngineImpl();
  public static final String SCRIPT = "__script";

  private final GlobalContext globalContext;

  @Override
  public Map<String, Serializable> execute(TaskRequest taskRequest) {

    try {
      final var transactionContext = this.globalContext.get(taskRequest.transactionId());
      final var metadata = taskRequest.metadata();
      final var payload = taskRequest.payload();

      if (!metadata.containsKey(SCRIPT)) {
        throw new WorkflowException("script not found");
      }

      final var response = new HashMap<String, Serializable>();
      final var decode = Base64.getDecoder().decode(metadata.get(SCRIPT).toString());
      final var script = new String(decode, StandardCharsets.UTF_8);
      final var context =
          new SimpleBindings(
              Map.of(
                  METADATA, metadata,
                  PAYLOAD, payload,
                  CONTEXT, transactionContext,
                  RESPONSE, response));
      groovyScriptEngine.eval(script, context);
      return response;
    } catch (IllegalArgumentException e) {
      log.error(e.getMessage());
      throw new WorkflowException("Base64 script is invalid");
    } catch (ScriptException e) {
      final var rootCause = getRootCause(e);
      log.error(rootCause.getMessage());
      throw new WorkflowException(rootCause.getMessage());
    }
  }

  public static Throwable getRootCause(Throwable throwable) {
    Throwable rootCause = throwable;
    while (rootCause.getCause() != null) {
      rootCause = rootCause.getCause();
    }
    return rootCause;
  }

  @Override
  public String getKey() {
    return "groovy";
  }
}
