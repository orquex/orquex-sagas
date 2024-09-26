package co.orquex.sagas.task.http.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public final class TestHttpClient {

  private final TestHttpResponse expectedResponse;
  private String url;
  private Map<String, String> headers;
  private Object body;

  public TestHttpClient(TestHttpResponse expectedResponse) {
    this.expectedResponse = expectedResponse;
  }

  public TestHttpClient post() {
    return this;
  }

  public TestHttpClient url(String url) {
    this.url = url;
    return this;
  }

  public TestHttpClient headers(Map<String, String> headers) {
    this.headers = headers;
    return this;
  }

  public TestHttpClient body(Object body) {
    this.body = body;
    return this;
  }

  public TestHttpResponse execute() {
    return expectedResponse;
  }

  public record TestHttpResponse(int code, Map<String, Serializable> body, Map<String, List<String>> headers) {}
}
