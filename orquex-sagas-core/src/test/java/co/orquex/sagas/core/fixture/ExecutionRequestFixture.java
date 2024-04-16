package co.orquex.sagas.core.fixture;

import co.orquex.sagas.domain.execution.ExecutionRequest;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecutionRequestFixture {

    private static final String FLOW_ID = UUID.randomUUID().toString();
    private static final String CORRELATION_ID = UUID.randomUUID().toString();

    public static ExecutionRequest getExecutionRequest(Map<String, Serializable> metadata,
            Map<String, Serializable> payload) {
        return new ExecutionRequest(FLOW_ID, CORRELATION_ID, metadata, payload);
    }

    public static ExecutionRequest getExecutionRequest() {
        return getExecutionRequest(Collections.emptyMap(), Collections.emptyMap());
    }
}
