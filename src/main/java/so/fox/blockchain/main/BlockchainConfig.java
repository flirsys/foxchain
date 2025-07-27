package so.fox.blockchain.main;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import so.fox.blockchain.core.Node;

@Configuration
public class BlockchainConfig {

  @Value("${blockchain.node.port:8080}")
  private int nodePort;

  @Bean
  public Node node() {
    return new Node(nodePort);
  }
}