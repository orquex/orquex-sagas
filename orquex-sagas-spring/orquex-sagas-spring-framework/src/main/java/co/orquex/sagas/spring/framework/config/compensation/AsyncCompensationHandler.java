package co.orquex.sagas.spring.framework.config.compensation;

import co.orquex.sagas.domain.transaction.Compensation;
import java.util.function.Consumer;

/** Handles asynchronous compensation actions. */
public interface AsyncCompensationHandler extends Consumer<Compensation> {}
