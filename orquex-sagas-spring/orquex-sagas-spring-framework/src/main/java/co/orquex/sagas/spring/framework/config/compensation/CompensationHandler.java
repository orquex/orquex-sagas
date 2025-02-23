package co.orquex.sagas.spring.framework.config.compensation;

import co.orquex.sagas.domain.transaction.Compensation;
import java.util.function.Consumer;

/** Handles compensation actions. */
public interface CompensationHandler extends Consumer<Compensation> {}
