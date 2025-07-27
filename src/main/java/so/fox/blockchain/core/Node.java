package so.fox.blockchain.core;

import org.iq80.leveldb.*;
import static org.iq80.leveldb.impl.Iq80DBFactory.*;
import static so.fox.blockchain.core.CurrencyConstants.UNITS_PER_MAIN_UNIT;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import so.fox.blockchain.util.SerializationUtils;
import so.fox.blockchain.wallet.Account;
import so.fox.blockchain.wallet.Wallet;

public class Node {
  private final List<Block> blockchain;
  private final List<Transaction> pendingTransactions;
  private final Map<String, Account> accounts;
  private final DB db;
  private final Wallet nodeWallet;
  private final int port;
  private final int difficulty = 4;
  private final long miningReward = 10 * UNITS_PER_MAIN_UNIT;

  public Node(int port) {
    this.port = port;
    String thisDir = "blockchaindb_" + port;
    this.blockchain = new CopyOnWriteArrayList<>();
    this.pendingTransactions = new CopyOnWriteArrayList<>();
    this.accounts = new HashMap<>();
    try {
      String classPath = System.getProperty("java.class.path");
      if (classPath == null || classPath.isEmpty()) {
        classPath = System.getProperty("user.dir");
        if (classPath == null || classPath.isEmpty()) {
          throw new RuntimeException("Не удалось определить путь: java.class.path и user.dir пусты");
        }
      }
      String[] paths = classPath.split(File.pathSeparator);
      File jarDir;
      File firstPath = new File(paths[0]).getAbsoluteFile();
      if (firstPath.isFile() && paths[0].endsWith(".jar")) jarDir = firstPath.getParentFile();
      else jarDir = new File(System.getProperty("user.dir")).getAbsoluteFile();
      if (jarDir == null || !jarDir.exists()) {
        throw new RuntimeException("Родительская директория не существует: " + firstPath.getAbsolutePath());
      }
      File newFolder = new File(jarDir, thisDir);
      if (!newFolder.exists()) {
        boolean created = newFolder.mkdirs();
        if (!created) {
          throw new RuntimeException("Не удалось создать директорию: " + newFolder.getAbsolutePath());
        }
      }
      
      String walletPath = new File(newFolder, "nodewallet.dat").getAbsolutePath();
      Wallet wallet;
      File file = new File(walletPath);
      if (file.exists())  wallet = loadWallet(walletPath);
      else {
        wallet = new Wallet();
        saveWallet(walletPath, wallet);
      }
      this.nodeWallet = wallet;
      
      Options options = new Options();
      options.createIfMissing(true);
      this.db = factory.open(newFolder, options);
      loadState();
    } catch (IOException e) {
      throw new RuntimeException("Error initializing LevelDB: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new RuntimeException("Error initializing Node: " + e.getMessage(), e);
    }
    if (blockchain.isEmpty()) {
      createGenesisBlock();
    }
  }

  private void saveWallet(String filePath, Wallet wallet) {
    try (PrintWriter out = new PrintWriter(filePath, "UTF-8")) {
      out.println(Base64.getEncoder().encodeToString(wallet.getPrivateKey().getEncoded()));
      out.println(Base64.getEncoder().encodeToString(wallet.getPublicKey().getEncoded()));
      out.println(wallet.getAddress());
    } catch (Exception e) {
      throw new RuntimeException("Error saving wallet: " + e);
    }
  }

  private static Wallet loadWallet(String filePath) {
    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String privLine = br.readLine();
      String pubLine = br.readLine();
      String addressLine = br.readLine();
      byte[] privBytes = Base64.getDecoder().decode(privLine);
      byte[] pubBytes = Base64.getDecoder().decode(pubLine);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privBytes);
      PublicKey pub = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
      PrivateKey priv = kf.generatePrivate(privSpec);
      return new Wallet(pub, priv, addressLine);
    } catch (Exception e) {
      throw new RuntimeException("Error loading wallet: " + e);
    }
  }

  private void createGenesisBlock() {
    Block genesis = new Block("0", new ArrayList<>());
    genesis.mineBlock(difficulty);
    blockchain.add(genesis);
    saveBlock(genesis);
  }

  public String addTransaction(Transaction tx, PublicKey senderKey) {
    if (tx == null || senderKey == null) return "Отсутствует поле";
    if (!tx.getSender().equals(Wallet.getAddressFromPublicKey(senderKey))) return "Некорректный адрес";
    if (!tx.verify(senderKey)) return "Подпись неверна";
    Account sender = getOrCreateAccount(tx.getSender());
    getOrCreateAccount(tx.getRecipient());
    if (tx.getNonce() != sender.getNonce()) return "Некорректный nonce";
    if (tx.getSender().equalsIgnoreCase(tx.getRecipient())) return "Нельзя переводить себе";
    //System.out.println("s: " + sender.getBalance() + " need: " + (tx.getValue() + tx.getFee()));
    if (sender.getBalance() < tx.getValue() + tx.getFee()) return "Недостаточно средств с учётом комиссии";
    if (tx.getValue() < 1L) return "Минимальная сумма перевода — 1 атомарная единица";
    pendingTransactions.add(tx);
    System.out.println("new TX(" + tx.getValue() + " FOX from " + tx.getSender() + " to " + tx.getRecipient() + ")");
    return "";
  }

  public Block mineBlock() {
    Account miner = getOrCreateAccount(nodeWallet.getAddress());
    miner.addBalance(miningReward);
    saveAccount(miner);
    List<Transaction> blockTxs = new ArrayList<>(pendingTransactions);
    for (Transaction tx : blockTxs) {
      tx.setStatus(1);
      Account sender = getOrCreateAccount(tx.getSender());
      Account recipient = getOrCreateAccount(tx.getRecipient());
      sender.addBalance(-(tx.getValue() + tx.getFee()));
      sender.incrementNonce();
      recipient.addBalance(tx.getValue());
      saveAccount(sender);
      saveAccount(recipient);
      miner.addBalance(tx.getFee());
      saveAccount(miner);
    }
    Block block = new Block(getLatestBlock().getHash(), blockTxs);
    block.mineBlock(difficulty);
    blockchain.add(block);
    saveBlock(block);
    System.out.println("new Block(" + block.getHash() + ")");
    pendingTransactions.clear();
    return block;
  }

  public Account getOrCreateAccount(String address) {
    return accounts.computeIfAbsent(address, k -> {
      Account newAcc = new Account(address, 0, 0);
      saveAccount(newAcc);
      return newAcc;
    });
  }

  public Account getAccount(String address) {
    return accounts.get(address);
  }

  public List<Transaction> getAccountTransactions(String address) {
    List<Transaction> result = new ArrayList<>();
    for (Block block : blockchain) {
      for (Transaction tx : block.getTransactions()) {
        if (address.equals(tx.getSender()) || address.equals(tx.getRecipient())) {
          result.add(tx);
        }
      }
    }
    for (Transaction tx : pendingTransactions) {
      if (address.equals(tx.getSender()) || address.equals(tx.getRecipient())) {
        result.add(tx);
      }
    }
    return result;
  }

  public List<Transaction> getPendingTransactions() {
    return new ArrayList<>(pendingTransactions);
  }

  public Block getBlock(int number) {
    if (number >= 0 && number < blockchain.size()) {
      return blockchain.get(number);
    }
    return null;
  }

  public Block getLatestBlock() {
    return blockchain.get(blockchain.size() - 1);
  }

  private void loadState() {
    try (DBIterator iterator = db.iterator()) {
      iterator.seekToFirst();
      int blockCnt = 0;
      while (iterator.hasNext()) {
        Map.Entry<byte[], byte[]> entry = iterator.next();
        String key = asString(entry.getKey());
        if (key.startsWith("block_")) {
          blockchain.add((Block) SerializationUtils.deserialize(entry.getValue()));
          blockCnt++;
        } else if (key.startsWith("account_")) {
          Account acc = (Account) SerializationUtils.deserialize(entry.getValue());
          accounts.put(acc.getAddress(), acc);
        }
      }
      blockchain.sort(Comparator.comparingLong(Block::getTimestamp));
      System.out.println("Loaded " + blockCnt + " blocks from DB");
    } catch (Exception e) {
      System.err.println("Error loading state from DB: " + e.getMessage());
    }
  }

  private void saveBlock(Block block) {
    try {
      db.put(bytes("block_" + block.getHash()), SerializationUtils.serialize(block));
    } catch (IOException e) {
      System.err.println("Error saving block: " + e.getMessage());
    }
  }

  private void saveAccount(Account acc) {
    try {
      db.put(bytes("account_" + acc.getAddress()), SerializationUtils.serialize(acc));
    } catch (IOException e) {
      System.err.println("Error saving account: " + e.getMessage());
    }
  }

  public void close() {
    try {
      if (db != null) db.close();
    } catch (IOException e) {
      System.err.println("Error closing LevelDB: " + e.getMessage());
    }
  }

  public Wallet getNodeWallet() { return nodeWallet; }
  public String getNodeAddress() { return nodeWallet.getAddress(); }
  public List<Block> getBlockchain() { return blockchain; }
  public int getPort() { return port; }
}