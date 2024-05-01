package co.orquex.sagas.sample.service;

import co.orquex.sagas.domain.api.TaskImplementation;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class GetUserStage implements TaskImplementation {

    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Serializable> execute(String transactionId,
            Map<String, Serializable> metadata, Map<String, Serializable> payload) {
        if (!payload.containsKey("user") && !metadata.containsKey("host")){
          throw new IllegalArgumentException("Payload must contain 'user' and Metadata a 'host'.");
        }
        // From task metadata get the host
        final var host = metadata.get("host").toString();
        final var userPayload = payload.get("user");
        // Get id from payload
        final var id = objectMapper.convertValue(userPayload, User.class).id();
        final var user = RestClient.create(host)
                .get()
                .uri("/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(User.class);
        if (user == null) throw new RuntimeException("User not found");
        return Map.of("user", user);
    }

    @Override
    public String getName() {
        return "get-user";
    }

    public record User (int id, String name, String username) implements Serializable {
        @Serial
        private static final long serialVersionUID = -1055166841112312527L;
    }
}
