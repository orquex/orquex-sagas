package co.orquex.sagas.task.jsonata;

import static java.util.Objects.nonNull;

import co.orquex.sagas.domain.api.context.TransactionContext;
import com.api.jsonata4java.expressions.EvaluateRuntimeException;
import com.api.jsonata4java.expressions.ExpressionsVisitor;
import com.api.jsonata4java.expressions.functions.FunctionBase;
import com.api.jsonata4java.expressions.generated.MappingExpressionParser.Function_callContext;
import com.api.jsonata4java.expressions.utils.Constants;
import com.api.jsonata4java.expressions.utils.FunctionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * Implements the $context function in JSONata. This function allows access to the current
 * transaction context.
 *
 * <p>Example:
 *
 * <p>$context("key") - returns the value of the key in the transaction context
 */
public class ContextFunction extends FunctionBase {

  public static final String FUNCTION_NAME = "$context";
  public static final String ERR_BAD_CONTEXT =
      String.format(Constants.ERR_MSG_BAD_CONTEXT, FUNCTION_NAME);
  public static final String ERR_ARG1BAD_TYPE =
      String.format(Constants.ERR_MSG_ARG1_BAD_TYPE, FUNCTION_NAME);
  public static final String ERR_ARG2BAD_TYPE =
      String.format(Constants.ERR_MSG_ARG2_BAD_TYPE, FUNCTION_NAME);

  private final TransactionContext transactionContext;
  private final ObjectMapper objectMapper;

  public ContextFunction(TransactionContext transactionContext, ObjectMapper objectMapper) {
    super();
    this.transactionContext = transactionContext;
    this.objectMapper = objectMapper;
  }

  public JsonNode invoke(ExpressionsVisitor expressionsVisitor, Function_callContext ctx) {

    final var argString = getArgumentString(expressionsVisitor, ctx);
    final var argCount = getArgumentCount(ctx, argString);

    return switch (argCount) {
      case 1 -> handleSingleArgument(argString);
      case 2 -> handleTwoArguments(expressionsVisitor, ctx);
      default ->
          throw new EvaluateRuntimeException(argCount == 0 ? ERR_BAD_CONTEXT : ERR_ARG2BAD_TYPE);
    };
  }

  private JsonNode getArgumentString(
      ExpressionsVisitor expressionsVisitor, Function_callContext ctx) {
    boolean useContext = FunctionUtils.useContextVariable(this, ctx, getSignature());
    JsonNode argString = JsonNodeFactory.instance.nullNode();
    if (useContext) {
      argString = FunctionUtils.getContextVariable(expressionsVisitor);
      if (argString == null || argString.isNull()) {
        useContext = false;
      }
    }
    if (!useContext) {
      argString = FunctionUtils.getValuesListExpression(expressionsVisitor, ctx, 0);
    }
    return argString;
  }

  private int getArgumentCount(Function_callContext ctx, JsonNode argString) {
    int argCount = getArgumentCount(ctx);
    if (argString != null && !argString.isNull()) {
      argCount++;
    }
    return argCount;
  }

  private JsonNode handleSingleArgument(JsonNode argString) {
    if (argString == null) {
      return null;
    }
    if (argString.isTextual()) {
      final String str = argString.textValue();
      final var contextResult = transactionContext.get(str);
      if (nonNull(contextResult)) return objectMapper.convertValue(contextResult, JsonNode.class);
      return null;
    } else {
      throw new EvaluateRuntimeException(ERR_ARG1BAD_TYPE);
    }
  }

  private JsonNode handleTwoArguments(
      ExpressionsVisitor expressionsVisitor, Function_callContext ctx) {
    final var value = ctx.exprValues().exprList();
    final var result = expressionsVisitor.visit(value);
    if (result != null && result.isTextual()) {
      final var contextResult = transactionContext.get(result.textValue());
      if (nonNull(contextResult)) return objectMapper.convertValue(contextResult, JsonNode.class);
      return null;
    } else {
      throw new EvaluateRuntimeException(ERR_ARG2BAD_TYPE);
    }
  }

  @Override
  public int getMaxArgs() {
    return 1;
  }

  @Override
  public int getMinArgs() {
    return 0; // account for context variable
  }

  @Override
  public String getSignature() {
    // accepts a single string and returns a string
    return "s-:x";
  }
}
