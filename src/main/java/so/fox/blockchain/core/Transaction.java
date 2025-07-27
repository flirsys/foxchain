package so.fox.blockchain.core;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

public class Transaction implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String sender;
  private final String recipient;
  private final long value;
  private final long fee;
  private final long timestamp;
  private final long nonce;
  private final String comment;
  private final String signature;
  private int status;  // 0 - pending | 1 - accepted | 2 - rejected

  public Transaction(String sender, String recipient, long value, long fee, long nonce, String comment, String signature) {
    this.sender = sender;
    this.recipient = recipient;
    this.value = value;
    this.fee = fee;
    this.timestamp = System.currentTimeMillis();
    this.nonce = nonce;
    this.comment = comment;
    this.signature = signature;
    this.status = 0;
  }

  public boolean verify(PublicKey senderKey) {
    try {
      String data = sender + "|" + recipient + "|" + value + "|" + fee + "|" + nonce + "|" + comment;
      Signature sig = Signature.getInstance("SHA256withRSA");
      sig.initVerify(senderKey);
      sig.update(data.getBytes("UTF-8"));
      return sig.verify(Base64.getDecoder().decode(signature));
    } catch (Exception e) {
      return false;
    }
  }

  public String getHash() {
    try {
      String data = sender + "|" + recipient + "|" + value + "|" + fee + "|" + nonce + "|" + comment + signature;
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(data.getBytes("UTF-8"));
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

  public String getSender() { return sender; }
  public String getRecipient() { return recipient; }
  public long getValue() { return value; }
  public long getFee() { return fee; }
  public long getTimestamp() { return timestamp; }
  public long getNonce() { return nonce; }
  public String getComment() { return comment; }
  public String getSignature() { return signature; }
  public int getStatus() { return status; }
  public void setStatus(int s) { this.status = s; }
}