package so.fox.blockchain.wallet;

import java.io.Serializable;

public class Account implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String address;
  private long balance;
  private long nonce;

  public Account(String address, long balance, long nonce) {
    if (address == null || address.isEmpty()) {
      throw new IllegalArgumentException("Account address cannot be empty");
    }
    this.address = address;
    this.balance = balance;
    this.nonce = nonce;
  }

  public String getAddress() {
    return address;
  }

  public long getBalance() {
    return balance;
  }

  public void setBalance(long balance) {
    this.balance = balance;
  }
  
  public void addBalance(long amount) {
    this.balance += amount;
  }

  public long getNonce() {
    return nonce;
  }

  public void incrementNonce() {
    this.nonce++;
  }
}