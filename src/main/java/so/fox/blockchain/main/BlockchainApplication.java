package so.fox.blockchain.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "so.fox.blockchain")
public class BlockchainApplication {

  public static void main(String[] args) {
    SpringApplication.run(BlockchainApplication.class, args);
  }
}