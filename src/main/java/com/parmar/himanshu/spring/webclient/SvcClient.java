package com.parmar.himanshu.spring.webclient;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class SvcClient {
  private static final Logger log = LoggerFactory.getLogger(SvcClient.class);
  private final WebClient svcWebClientWithConnectionPool;
  private final WebClient svcWebClientNoConnectionPool;

  private final HttpClient nativeHttpClient;

  public SvcClient(
      @Qualifier("svcWebClientWithConnectionPool") WebClient svcWebClientWithConnectionPool,
      @Qualifier("svcWebClientNoConnectionPool") WebClient svcWebClientNoConnectionPool) {
    this.svcWebClientWithConnectionPool = svcWebClientWithConnectionPool;
    this.svcWebClientNoConnectionPool = svcWebClientNoConnectionPool;
    this.nativeHttpClient = HttpClient.newHttpClient();
  }

  public Integer callWithWebclient(String payload) {
    log.info("calling webclient with new connection all time");
    SampleRequest req = new SampleRequest(UUID.randomUUID().toString(), payload);
    return svcWebClientNoConnectionPool
        .post()
        .uri("/payload-size")
        .body(Mono.just(req), SampleRequest.class)
        .retrieve()
        .bodyToMono(Integer.class)
        .block();
  }

  public Integer callWithWebclientWithConnectionPool(String payload) {
    log.info("calling webclient with connection pool");
    SampleRequest req = new SampleRequest(UUID.randomUUID().toString(), payload);
    return svcWebClientWithConnectionPool
        .post()
        .uri("/payload-size")
        .body(Mono.just(req), SampleRequest.class)
        .retrieve()
        .bodyToMono(Integer.class)
        .block();
  }

  public Integer callWithNativeHttpClient(String payload) {
    try {
      log.info("calling with java native http client");
      SampleRequest req = new SampleRequest(UUID.randomUUID().toString(), payload);
      // ensure jackson is able to serialize large json
      ObjectMapper om = new ObjectMapper();
      om.getFactory()
          .setStreamReadConstraints(
              StreamReadConstraints.builder().maxStringLength(Integer.MAX_VALUE).build());
      String reqJson = om.writeValueAsString(req);
      HttpRequest httpRequest =
          HttpRequest.newBuilder()
              .uri(new URI("http://localhost:8000/payload-size"))
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .POST(HttpRequest.BodyPublishers.ofString(reqJson))
              .build();
      HttpResponse<String> response =
          nativeHttpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
      log.info("http response status={} body={}", response.statusCode(), response.body());
      if (response.statusCode() == 200) {
        return Integer.parseInt(response.body());
      } else {
        throw new RuntimeException("Failed with http=" + response.statusCode() + " error=" + response.body());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
