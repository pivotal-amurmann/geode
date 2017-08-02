package org.apache.geode.internal.cache.tier.sockets.sasl;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import com.sun.org.apache.xpath.internal.operations.String;
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.internal.HeapDataOutputStream;
import org.apache.geode.internal.InternalDataSerializer;
import org.apache.geode.test.junit.categories.UnitTest;

@Category(UnitTest.class)
public class SaslMessengerTest {

  private DataInputStream inputStream;
  private HeapDataOutputStream outputStream;
  private SaslMessenger saslMessenger;
  private byte[] message;
  private byte[] outPutArray;

  @Before
  public void setup() {
    message = new byte[]{2, 2};
    outPutArray = new byte[1000];
    outputStream = new HeapDataOutputStream(outPutArray);
    saslMessenger = new SaslMessenger(inputStream, outputStream);
  }

  @Test
  public void sendMessageWritesMessageToStream() throws IOException {
    saslMessenger.sendMessage(message);
    ByteArrayInputStream
        byteArrayInputStream =
        new ByteArrayInputStream(outputStream.toByteArray());
    DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
    int arrayLength = dataInputStream.readInt();
    byte[] actualWrittenBytes = new byte[arrayLength];
    dataInputStream.read(actualWrittenBytes);

    assertArrayEquals(message, actualWrittenBytes);
  }

  @Test
  public void readMessageReturnsTheNextMessage() throws IOException {
    HeapDataOutputStream tempOutputStream = new HeapDataOutputStream(outPutArray);
    tempOutputStream.writeInt(message.length);
    tempOutputStream.write(message);
    byte[] serializedBytes = tempOutputStream.toByteArray();

    inputStream = new DataInputStream(new ByteInputStream(serializedBytes, serializedBytes.length));
    SaslMessenger saslMessenger = new SaslMessenger(inputStream, tempOutputStream);

    byte[] readMessage = saslMessenger.readMessage();
    assertArrayEquals(message, readMessage);
  }
}
