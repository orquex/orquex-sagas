package co.orquex.sagas.core.flow;

import co.orquex.sagas.domain.api.AsyncExecutable;
import co.orquex.sagas.domain.api.repository.FlowRepository;
import co.orquex.sagas.domain.api.repository.TransactionRepository;

/**
 * Abstract base class for asynchronous workflow executors that provides a common capability or
 * feature for executing workflows using an event approach.
 *
 * @param <T> the type of the input received.
 */
abstract class AbstractAsyncExecutable<T> extends AbstractWorkflowExecutor
    implements AsyncExecutable<T> {

  protected AbstractAsyncExecutable(
      FlowRepository flowRepository, TransactionRepository transactionRepository) {
    super(flowRepository, transactionRepository);
  }
}
