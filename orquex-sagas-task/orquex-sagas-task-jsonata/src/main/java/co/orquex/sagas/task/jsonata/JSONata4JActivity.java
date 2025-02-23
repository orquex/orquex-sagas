package co.orquex.sagas.task.jsonata;

import static java.util.Objects.isNull;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.api.context.GlobalContext;
import co.orquex.sagas.domain.exception.WorkflowException;
import co.orquex.sagas.domain.task.TaskRequest;
import com.api.jsonata4java.expressions.EvaluateException;
import com.api.jsonata4java.expressions.Expressions;
import com.api.jsonata4java.expressions.FrameEnvironment;
import com.api.jsonata4java.expressions.ParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JSONata4JActivity implements TaskImplementation {

  public static final String EXPRESSION = "__expression";
  public static final String RESULT = "__result";
  public static final String METADATA_INPUT_KEY = "metadata";
  public static final String PAYLOAD_INPUT_KEY = "payload";
  private final GlobalContext globalContext;
  private final ObjectMapper objectMapper;

  @Override
  public Map<String, Serializable> execute(TaskRequest taskRequest) {
    try {
      final var transactionContext = this.globalContext.get(taskRequest.transactionId());
      final var metadata = new HashMap<>(taskRequest.metadata());
      final var payload = new HashMap<>(taskRequest.payload());

      // Checks if the metadata contains the key "__expression"
      if (!metadata.containsKey(EXPRESSION)) {
        throw new WorkflowException("JSONata expression not found");
      }
      final var base64Expression = metadata.remove(EXPRESSION);
      final var expression =
          new String(
              java.util.Base64.getDecoder()
                  .decode(base64Expression.toString().getBytes(StandardCharsets.UTF_8)));
      final var input = Map.of(METADATA_INPUT_KEY, metadata, PAYLOAD_INPUT_KEY, payload);
      // Converts the payload to a JsonNode
      final var jsonNode = objectMapper.convertValue(input, JsonNode.class);
      // Parses the JSONata expression
      final var jsonata = Expressions.parse(expression);
      // Sets the context function
      final FrameEnvironment environment = jsonata.getEnvironment();
      environment.setJsonataFunction(
          ContextFunction.FUNCTION_NAME, new ContextFunction(transactionContext, objectMapper));
      // Evaluates the JSONata expression
      final var result = jsonata.evaluate(jsonNode);
      // Processes the result
      return processResult(result);
    } catch (IllegalArgumentException e) {
      log.error(e.getMessage(), e);
      throw new WorkflowException("An error occurred while decoding the JSONata expression");
    } catch (IOException | ParseException e) {
      log.error(e.getMessage(), e);
      throw new WorkflowException("An error occurred while parsing the JSONata expression");
    } catch (EvaluateException e) {
      log.error(e.getMessage(), e);
      throw new WorkflowException("An error occurred while evaluating the JSONata expression");
    }
  }

  private Map<String, Serializable> processResult(JsonNode result) {
    if (isNull(result) || result.isNull()) {
      final Map<String, Serializable> resultMap = HashMap.newHashMap(1);
      resultMap.put(RESULT, null);
      return resultMap;
    } else if (result.isObject()) {
      return objectMapper.convertValue(result, new TypeReference<>() {});
    } else if (result.isArray()) {
      final var list =
          objectMapper.convertValue(result, new TypeReference<ArrayList<Serializable>>() {});
      return Map.of(RESULT, list);
    } else if (result.isTextual()) {
      return Map.of(RESULT, result.textValue());
    } else if (result.isNumber()) {
      return Map.of(RESULT, result.numberValue());
    } else {
      return Map.of(RESULT, result.booleanValue());
    }
  }

  @Override
  public String getKey() {
    return "jsonata4j";
  }
}
