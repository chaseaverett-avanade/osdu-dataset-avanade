package org.opengroup.osdu.delivery.provider.reference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"org.opengroup.osdu"})
public class DeliveryReferenceApplication {
  public static void main(String[] args) {
    SpringApplication.run(DeliveryReferenceApplication.class, args);
  }
}
