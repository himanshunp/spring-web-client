package com.parmar.himanshu.spring.webclient;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
  private static final Logger log = LoggerFactory.getLogger(Controller.class);

  @Observed
  @PostMapping("/payload-size")
  public int payLoadSize(@RequestBody SampleRequest request) {
    log.info("got request");
    return request.payload().length();
  }
}
