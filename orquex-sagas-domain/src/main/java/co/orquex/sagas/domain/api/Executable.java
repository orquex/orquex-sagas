package co.orquex.sagas.domain.api;

public interface Executable<I, O> {

  O execute(I request);
}
