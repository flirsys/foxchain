package so.fox.blockchain.core;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class Block implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private final String previousHash;
  private String hash;
  private final long timestamp;
  private int nonce;
  private final List<Transaction> transactions;
  private final String merkleRoot;

  public Block(String previousHash, List<Transaction> transactions) {
    this.previousHash = previousHash;
    this.transactions = transactions;
    this.timestamp = System.currentTimeMillis();
    this.nonce = 0;
    this.merkleRoot = calculateMerkleRoot();
    this.hash = calculateHash();
  }

  public String calculateHash() {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String blockData = previousHash + timestamp + nonce + merkleRoot;
      byte[] hashBytes = digest.digest(blockData.getBytes("UTF-8"));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String calculateMerkleRoot() {
    if (transactions.isEmpty()) {
      return "0";
    }
    List<String> tree = new ArrayList<>();
    for (Transaction tx : transactions) {
      tree.add(tx.getHash());
    }
    while (tree.size() > 1) {
      List<String> newTree = new ArrayList<>();
      for (int i = 0; i < tree.size(); i += 2) {
        String left = tree.get(i);
        String right = (i + 1 < tree.size()) ? tree.get(i + 1) : left;
        newTree.add(hashPair(left, right));
      }
      tree = newTree;
    }
    return tree.get(0);
  }

  private String hashPair(String left, String right) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest((left + right).getBytes("UTF-8"));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void mineBlock(int difficulty) {
    String target = new String(new char[difficulty]).replace('\0', '0');
    while (!hash.substring(0, difficulty).equals(target)) {
      nonce++;
      hash = calculateHash();
    }
  }

  public String getHash() { return hash; }
  public String getPreviousHash() { return previousHash; }
  public long getTimestamp() { return timestamp; }
  public List<Transaction> getTransactions() { return new ArrayList<>(transactions); }
}