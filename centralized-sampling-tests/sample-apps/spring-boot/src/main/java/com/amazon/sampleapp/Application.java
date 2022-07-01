package com.amazon.sampleapp;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

  public static void main(String[] args) {
    // listenAddress should consist host + port (e.g. 127.0.0.1:5000)
    String port;
    String host;
    String listenAddress = System.getenv("LISTEN_ADDRESS");

    if (listenAddress == null) {
      host = "127.0.0.1";
      port = "8080";
    } else {
      String[] splitAddress = listenAddress.split(":");
      host = splitAddress[0];
      port = splitAddress[1];
    }

    Map<String, Object> config = new HashMap<String, Object>();
    config.put("server.address", host);
    config.put("server.port", port);

    SpringApplication app = new SpringApplication(Application.class);
    app.setDefaultProperties(config);
    app.run(args);
  }
}
