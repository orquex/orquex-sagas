package co.orquex.sagas.task.okhttp;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** Represents the response of an HTTP operation using OkHttp client. */
record OkHttpActivityResponse(
    int code, Map<String, Serializable> body, Map<String, List<String>> headers) {}
