package co.orquex.sagas.task.jsonata;

import static co.orquex.sagas.task.jsonata.JSONata4JActivity.EXPRESSION;
import static co.orquex.sagas.task.jsonata.JSONata4JActivity.RESULT;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import co.orquex.sagas.domain.api.TaskImplementation;
import co.orquex.sagas.domain.api.context.GlobalContext;
import co.orquex.sagas.domain.task.TaskRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

class JSONata4JActivityTest {

  public static final String TRANSACTION_ID = UUID.randomUUID().toString();
  static TaskImplementation jsonata;
  static Map<String, Serializable> payload;
  static GlobalContext globalContext;

  @BeforeAll
  static void beforeAll() throws IOException {
    globalContext = Mockito.mock(GlobalContext.class, Mockito.RETURNS_DEEP_STUBS);
    final var objectMapper = new ObjectMapper();
    jsonata = new JSONata4JActivity(globalContext, objectMapper);
    final var exampleJson = Files.readString(Path.of("src/test/resources/example.json"));
    payload = objectMapper.readValue(exampleJson, new TypeReference<>() {});
  }

  @AfterEach
  void tearDown() {
    Mockito.reset(globalContext);
  }

  @ParameterizedTest
  @MethodSource("provideSingleResults")
  void shouldReturnSingleDataType(String expression, Serializable expected) {
    // When
    final var result = test(expression);
    // Then
    assertThat(result).containsEntry(RESULT, expected);
  }

  private static Stream<Arguments> provideSingleResults() {
    return Stream.of(
        Arguments.of("payload.Surname", "Smith"),
        Arguments.of("payload.Age", 28),
        Arguments.of("payload.Address.City", "Winchester"),
        Arguments.of("payload.Other.`Over 18 ?`", true),
        Arguments.of("payload.(Phone.number)[0]", "0203 544 1234"),
        Arguments.of("payload.Phone[0].number", "0203 544 1234"),
        Arguments.of("payload.Phone[type='mobile'].number", "077 7700 1234"),
        Arguments.of("payload.FirstName & ' ' & payload.Surname", "Fred Smith"),
        Arguments.of("payload.Address.(Street & ', ' & City)", "Hursley Park, Winchester"));
  }

  @Test
  void shouldFilterArray() {
    // When
    final var result = test("payload.Phone[0]");
    // Then
    assertThat(result)
        .containsAnyOf(Map.entry("type", "home"), Map.entry("number", "0203 544 1234"));
  }

  @Test
  void shouldReturnArray() {
    // When
    final var result = test("payload.Phone.number");
    // Then
    final var expected =
        new ArrayList<>(List.of("0203 544 1234", "01962 001234", "01962 001235", "077 7700 1234"));
    assertThat(result).containsKey(RESULT).containsValue(expected);
  }

  @Test
  void shouldTransformWithTransactionContext() {
    // Given
    final var transactionId = TRANSACTION_ID;
    when(globalContext.get(transactionId).get(anyString())).thenReturn("context-value");
    final var expression =
        """
            {
              "key": $context('context-key'),
              "firstName": $context(payload.FirstName),
              "headers": {
                "Authorization": 'Bearer ' & metadata.__accessKey
              }
            }
        """;
    final var result = test(transactionId, expression, Map.of("__accessKey", "key"), payload);
    // Then
    final var headers = new HashMap<>(Map.of("Authorization", "Bearer key"));
    assertThat(result)
        .containsAnyOf(
            Map.entry("key", "context-value"),
            Map.entry("firstName", "Fred"),
            Map.entry("headers", headers));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "notExists",
        "$context('notExists')",
        "$context(payload.NoExists)",
        "$context(payload.FirstName)"
      })
  void shouldNullResultWhenExpressionResultIsNull(String expression) {
    // When
    when(globalContext.get(anyString()).get(anyString())).thenReturn(null);
    final var result = test(expression);
    // Then
    assertThat(result).containsEntry(RESULT, null);
  }

  @ParameterizedTest
  @ValueSource(strings = {"$context()", "$context('1','2','3')", "$context(1)", "$context(payload.Other.Misc)"})
  void shouldThrowExceptionWhenExpressionContextParameterIsInvalid(String expression) {
    // When / Then
    assertThatThrownBy(() -> test(expression))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("An error occurred while evaluating the JSONata expression");
  }

  @Test
  void shouldThrowExceptionWhenBase64ExpressionIsInvalid() {
    // When
    final var metadata = Map.of(EXPRESSION, "invalid-base64");
    final var taskRequest = new TaskRequest(TRANSACTION_ID, metadata, payload);
    // Then
    assertThatThrownBy(() -> jsonata.execute(taskRequest))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("An error occurred while decoding the JSONata expression");
  }

  @ParameterizedTest
  @ValueSource(strings = {".", "payload.", "payload..", "payload..Address", "${test}"})
  void shouldThrowExceptionWhenExpressionIsInvalid(String expression) {
    // When / Then
    assertThatThrownBy(() -> test(expression))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("An error occurred while parsing the JSONata expression");
  }

  @Test
  void shouldThrowExceptionWhenExpressionNotFound() {
    // When
    final var taskRequest = new TaskRequest(TRANSACTION_ID, Map.of(), payload);
    // Then
    assertThatThrownBy(() -> jsonata.execute(taskRequest))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("JSONata expression not found");
  }

  @Test
  void shouldReturnTaskImplementationKey() {
    // When
    final var key = jsonata.getKey();
    // Then
    assertThat(key).isEqualTo("jsonata4j");
  }

  static Map<String, Serializable> test(
      String transactionId,
      String expression,
      Map<String, Serializable> metadata,
      Map<String, Serializable> payload) {
    final var base64Expression = Base64.getEncoder().encodeToString(expression.getBytes());
    final var metadataWithExpression = new HashMap<>(metadata);
    metadataWithExpression.put(EXPRESSION, base64Expression);
    final var taskRequest = new TaskRequest(transactionId, metadataWithExpression, payload);
    return jsonata.execute(taskRequest);
  }

  static Map<String, Serializable> test(String expression) {
    return test(TRANSACTION_ID, expression, Collections.emptyMap(), payload);
  }
}
