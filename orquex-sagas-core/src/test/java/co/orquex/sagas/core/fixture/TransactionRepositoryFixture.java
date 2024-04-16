package co.orquex.sagas.core.fixture;

import co.orquex.sagas.domain.repository.TransactionRepository;
import co.orquex.sagas.domain.transaction.Transaction;

public class TransactionRepositoryFixture implements TransactionRepository {

    @Override
    public boolean existByFlowIdAndCorrelationId(String flowId, String correlationId) {
        return false;
    }

    @Override
    public Transaction save(Transaction transaction) {
        return transaction;
    }
}
