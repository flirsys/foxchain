package so.fox.blockchain.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import so.fox.blockchain.core.Block;
import so.fox.blockchain.core.Node;
import so.fox.blockchain.core.Transaction;
import so.fox.blockchain.wallet.Account;
import so.fox.blockchain.wallet.Wallet;

import java.math.BigDecimal;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static so.fox.blockchain.core.CurrencyConstants.UNITS_PER_MAIN_UNIT;
import static so.fox.blockchain.core.CurrencyConstants.DECIMALS;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class BlockchainController {

  private final Node node;
  private final long defaultFee = 50L;

  @Autowired
  public BlockchainController(Node node) {
    this.node = node;
  }

  private String formatAmount(long atomicAmount) {
    BigDecimal amount = BigDecimal.valueOf(atomicAmount);
    BigDecimal divisor = BigDecimal.valueOf(UNITS_PER_MAIN_UNIT);
    return amount.divide(divisor).setScale(DECIMALS).toPlainString();
  }

  private long parseAmountToAtomic(String amountStr) throws IllegalArgumentException {
    if (amountStr == null || amountStr.trim().isEmpty()) {
      throw new IllegalArgumentException("Amount cannot be empty");
    }
    String parsableAmount = amountStr.replace(',', '.');
    BigDecimal amount;
    try {
      amount = new BigDecimal(parsableAmount);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid number format for amount");
    }
    if (amount.signum() < 0) {
      throw new IllegalArgumentException("Amount cannot be negative");
    }
    if (amount.scale() > DECIMALS) {
      throw new IllegalArgumentException("Too many decimal places. Max allowed: " + DECIMALS);
    }
    return amount.multiply(BigDecimal.valueOf(1)).longValue();
  }

  @GetMapping("/account/{address}")
  public ResponseEntity<Object> getAccount(@PathVariable String address) {
    if ("node".equalsIgnoreCase(address)) {
      address = node.getNodeAddress();
    }
    Account acc = node.getAccount(address);
    if (acc == null) {
      return ResponseEntity.status(404).body(Map.of("error", "Account not found"));
    }
    return ResponseEntity.ok(Map.of(
      "address", acc.getAddress(),
      "balance", formatAmount(acc.getBalance()),
      "nanoBalance", acc.getBalance(),
      "nonce", acc.getNonce()
    ));
  }

  @GetMapping("/account/{address}/tx")
  public ResponseEntity<Object> getAccountTXs(@PathVariable String address) {
    if ("node".equalsIgnoreCase(address)) {
      address = node.getNodeAddress();
    }
    List<Transaction> txs = node.getAccountTransactions(address);
    return ResponseEntity.ok(Map.of(
      "address", address,
      "txs", txs
    ));
  }

  @PostMapping("/tx/send")
  public ResponseEntity<Object> sendTransaction(@RequestBody Map<String, String> request) {
    try {
      String sender = request.get("sender");
      String recipient = request.get("recipient");
      String valueStr = request.get("value");
      String feeStr = request.get("fee");
      String comment = request.getOrDefault("comment", "");
      String nonceStr = request.get("nonce");
      String signatureB64 = request.get("signature");
      String publicKeyB64 = request.get("publicKey");
      if (sender == null || recipient == null || signatureB64 == null || publicKeyB64 == null || valueStr == null || nonceStr == null) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing required fields."));
      }
      if (comment.length() > 255) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid transaction: comment too long"));
      }
      long value;
      long fee;
      try {
        value = parseAmountToAtomic(valueStr);
        fee = (feeStr == null || feeStr.trim().isEmpty()) ? defaultFee : parseAmountToAtomic(feeStr);
      } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid amount: " + e.getMessage()));
      }
      long nonce = Long.parseLong(nonceStr);
      PublicKey publicKey;
      Transaction tx;
      if ("node".equals(signatureB64) && "node".equals(publicKeyB64) && sender.equals(node.getNodeAddress())) {
        Wallet nodeWallet = node.getNodeWallet();
        String dataForSign = sender + "|" + recipient + "|" + value + "|" + fee + "|" + nonce + "|" + comment;
        String signatureFromNode = Base64.getEncoder().encodeToString(nodeWallet.sign(dataForSign));
        tx = new Transaction(sender, recipient, value, fee, nonce, comment, signatureFromNode);
        publicKey = nodeWallet.getPublicKey();
      } else {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyB64);
        publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        tx = new Transaction(sender, recipient, value, fee, nonce, comment, signatureB64);
      }
      String result = node.addTransaction(tx, publicKey);
      if (result.isEmpty()) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.schedule(() -> {
          List<Transaction> trx = node.getPendingTransactions();
          if (!trx.isEmpty()){
            node.mineBlock();
          }
        }, 10, TimeUnit.SECONDS);
        executor.shutdown();
        return ResponseEntity.ok(Map.of("success", true, "message", "Transaction added", "txHash", tx.getHash()));
      } else {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", result));
      }
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Error: " + e.getMessage()));
    }
  }

  @GetMapping("/tx/pending")
  public List<Transaction> getPendingTransactions() {
    return node.getPendingTransactions();
  }

  @PostMapping("/mine")
  public ResponseEntity<Object> mine() {
    try {
      Block block = node.mineBlock();
      return ResponseEntity.ok(Map.of(
        "success", true,
        "message", "Block mined successfully!",
        "blockHash", block.getHash(),
        "txCount", block.getTransactions().size()
      ));
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Mining error: " + e.getMessage()));
    }
  }

  @GetMapping("/block/latest")
  public Block getLatestBlock() {
    return node.getLatestBlock();
  }

  @GetMapping("/block/{number}")
  public ResponseEntity<Object> getBlock(@PathVariable int number) {
    Block block = node.getBlock(number);
    if (block == null) {
      return ResponseEntity.status(404).body(Map.of("error", "Block not found"));
    }
    return ResponseEntity.ok(block);
  }
}