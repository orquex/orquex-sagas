package co.orquex.sagas.sample.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/status")
public class HttpStatusController {

  private final Logger log = LoggerFactory.getLogger(HttpStatusController.class);

  @GetMapping("/{statusCode}")
  public ResponseEntity<String> status(@PathVariable("statusCode") String statusCode) {
    try {
      log.info("Received status code {}", statusCode);
      final var httpStatus = HttpStatus.valueOf(Integer.parseInt(statusCode));
      return ResponseEntity.status(httpStatus).build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
}
