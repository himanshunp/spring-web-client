package com.parmar.himanshu.spring.webclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.StreamReadConstraints;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureObservability
public class ControllerTest {
  @Autowired private SvcClient svcClient;

  @ParameterizedTest
  @ValueSource(
      ints = {
        1000000,
        StreamReadConstraints.DEFAULT_MAX_STRING_LEN + 1,
        StreamReadConstraints.DEFAULT_MAX_STRING_LEN
            + 100000000 // this will give reactor.netty.http.client.PrematureCloseException:
                        // Connection prematurely closed BEFORE response
      })
  void testWebClient(int payloadSize) {
    String payload = generatePayload(payloadSize);
    Integer size = svcClient.callWithWebclient(payload);
    assertEquals(payload.length(), size.intValue());
  }

  @ParameterizedTest
  @ValueSource(
      ints = {
        1000000,
        StreamReadConstraints.DEFAULT_MAX_STRING_LEN + 1,
        StreamReadConstraints.DEFAULT_MAX_STRING_LEN
            + 100000000 // this will give reactor.netty.http.client.PrematureCloseException:
                        // Connection prematurely closed BEFORE response
      })
  void testWebClientWithConnectionPool(int payloadSize) {
    String payload = generatePayload(payloadSize);
    Integer size = svcClient.callWithWebclientWithConnectionPool(payload);
    assertEquals(payload.length(), size.intValue());
  }

  @ParameterizedTest
  @ValueSource(
      ints = {
        1000000,
        StreamReadConstraints.DEFAULT_MAX_STRING_LEN + 1,
        StreamReadConstraints.DEFAULT_MAX_STRING_LEN
            + 100000000 // this gives broken pipe during chunking
      })
  void testNativeHttpClient(int payloadSize) {
    String payload = generatePayload(payloadSize);
    Integer size = svcClient.callWithNativeHttpClient(payload);
    assertEquals(payload.length(), size.intValue());
  }

  private String generatePayload(int size) {
    StringBuilder payload = new StringBuilder("begin");
    while (payload.length() < size) {
      payload.append("a");
    }
    return payload.toString();
  }
}
