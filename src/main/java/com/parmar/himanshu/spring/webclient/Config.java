package com.parmar.himanshu.spring.webclient;

import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import jdk.net.ExtendedSocketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

@Configuration
public class Config {
  private static final Logger log = LoggerFactory.getLogger(Config.class);
  public static final String BASE_URL = "http://localhost:8000";

  @Bean
  @Qualifier("httpClientWithConnectionPool")
  HttpClient httpClientWithConnectionPool() {
    // Creating based on all possible configs as per
    // https://projectreactor.io/docs/netty/release/reference/index.html#_timeout_configuration
    ConnectionProvider provider =
        ConnectionProvider.builder("custom")
            .maxConnections(50)
            .maxIdleTime(Duration.ofSeconds(20))
            .maxLifeTime(Duration.ofSeconds(60))
            .pendingAcquireTimeout(Duration.ofSeconds(60))
            .evictInBackground(Duration.ofSeconds(120))
            .build();
    // we'll also try tcp keep-alive as per
    // https://projectreactor.io/docs/netty/release/reference/index.html#connection-timeout
    return HttpClient.newConnection()
        .wiretap(
            "reactor.netty.http.client.httpclient", LogLevel.INFO, AdvancedByteBufFormat.SIMPLE)
        .responseTimeout(Duration.ofSeconds(600))
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 300)
        .option(ChannelOption.SO_KEEPALIVE, true)
        // following does not work on mac but works on linux
        // .option(EpollChannelOption.TCP_KEEPIDLE, 300)
        // .option(EpollChannelOption.TCP_KEEPINTVL, 60)
        // .option(EpollChannelOption.TCP_KEEPCNT, 8)
        // for mac
        .option(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPIDLE), 300)
        .option(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPINTERVAL), 60)
        .option(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPCOUNT), 8)
        .doOnConnected(
            conn -> {
              conn.addHandlerFirst(new ReadTimeoutHandler(250, TimeUnit.SECONDS));
              conn.addHandlerFirst(new WriteTimeoutHandler(250));
              log.info("doOnConnected");
            })
        .doOnRequestError(
            (httpClientRequest, throwable) ->
                log.info("got error req={}", httpClientRequest, throwable));
  }

  @Bean
  @Qualifier("svcWebClientWithConnectionPool")
  WebClient svcWebClientWithConnectionPool(
      WebClient.Builder builder,
      @Qualifier("httpClientWithConnectionPool") HttpClient httpClientWithConnectionPool) {
    return builder
        .baseUrl(BASE_URL)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(-1))
        .clientConnector(new ReactorClientHttpConnector(httpClientWithConnectionPool))
        .build();
  }

  @Bean
  @Qualifier("svcWebClientNoConnectionPool")
  WebClient svcWebClientNoConnectionPool(WebClient.Builder builder) {
    return builder
        .baseUrl(BASE_URL)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(-1))
        .clientConnector(new ReactorClientHttpConnector(HttpClient.newConnection()))
        .build();
  }
}
