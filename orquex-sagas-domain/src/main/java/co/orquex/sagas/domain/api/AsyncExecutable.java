package co.orquex.sagas.domain.api;

public interface AsyncExecutable<R> {

  void execute(R request);
}
