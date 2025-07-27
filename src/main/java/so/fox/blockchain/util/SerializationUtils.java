package so.fox.blockchain.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SerializationUtils {

  public static byte[] serialize(Object obj) throws IOException {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(obj);
      return baos.toByteArray();
    }
  }

  public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
         ObjectInputStream ois = new ObjectInputStream(bais)) {
      return ois.readObject();
    }
  }
}