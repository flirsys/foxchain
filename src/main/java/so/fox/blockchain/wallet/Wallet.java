package so.fox.blockchain.wallet;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

public class Wallet {

  private final PublicKey publicKey;
  private final PrivateKey privateKey;
  private final String address;

  public Wallet() {
    try {
      KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
      keyGen.initialize(2048);
      KeyPair pair = keyGen.generateKeyPair();
      this.publicKey = pair.getPublic();
      this.privateKey = pair.getPrivate();
      this.address = getAddressFromPublicKey(this.publicKey);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public Wallet(PublicKey publicKey, PrivateKey privateKey, String address) {
    this.publicKey = publicKey;
    this.privateKey = privateKey;
    this.address = address;
  }

  public static String getAddressFromPublicKey(PublicKey publicKey) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(publicKey.getEncoded());
      StringBuilder sb = new StringBuilder();
      for (byte b : hash) sb.append(String.format("%02x", b));
      char[] arr = sb.toString().toCharArray();
      int posF = (hash[0] & 0xFF) % 5;
      int posO = (hash[1] & 0xFF) % 5;
      while (posO == posF) posO = (posO + 1) % 5;
      int posX = (hash[2] & 0xFF) % 5;
      while (posX == posF || posX == posO) posX = (posX + 1) % 5;
      arr[posF] = 'f';
      arr[posO] = 'o';
      arr[posX] = 'x';
      return new String(arr);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public byte[] sign(String data) {
    try {
      Signature rsa = Signature.getInstance("SHA256withRSA");
      rsa.initSign(privateKey);
      rsa.update(data.getBytes("UTF-8"));
      return rsa.sign();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }

  public PrivateKey getPrivateKey() {
    return privateKey;
  }

  public String getAddress() {
    return address;
  }
}