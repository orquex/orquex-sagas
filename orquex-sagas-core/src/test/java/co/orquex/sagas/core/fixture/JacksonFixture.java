package co.orquex.sagas.core.fixture;

import co.orquex.sagas.domain.jackson.OrquexJacksonModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JacksonFixture {

  public static final String PATH = "./src/test/resources/%s";
  public static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();
    mapper.registerModule(new OrquexJacksonModule());
    mapper.registerModule(new JavaTimeModule());
  }

  public static <T> T readValue(String filename, Class<T> clss) {
    try {
      return JacksonFixture.mapper.readValue(
          Files.readString(Path.of(PATH.formatted(filename))), clss);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
