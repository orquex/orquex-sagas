package co.orquex.sagas.task.http.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** Represents the response of an HTTP operation using some client. */
public record HttpActivityResponse(
    int code, Map<String, Serializable> body, Map<String, List<String>> headers) {}
