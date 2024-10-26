package co.orquex.sagas.core.fixture;

import co.orquex.sagas.domain.task.Task;

public final class TaskFixture {

    public static Task getTask(String id) {
        return new Task(id, id, id, null, null, null);
    }
}
