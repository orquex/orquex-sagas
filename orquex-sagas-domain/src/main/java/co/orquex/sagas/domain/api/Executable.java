package co.orquex.sagas.domain.api;

public interface Executable<R> {

  void execute(R request);
}
