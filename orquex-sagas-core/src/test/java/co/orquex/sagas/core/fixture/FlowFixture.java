package co.orquex.sagas.core.fixture;

import static co.orquex.sagas.core.fixture.JacksonFixture.readValue;

import co.orquex.sagas.domain.flow.Flow;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlowFixture {

  public static Flow getFlow(String fileName) {
    return readValue(fileName, Flow.class);
  }
}
