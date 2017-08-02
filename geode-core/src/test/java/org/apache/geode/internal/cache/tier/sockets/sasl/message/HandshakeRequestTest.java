package org.apache.geode.internal.cache.tier.sockets.sasl.message;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.test.junit.categories.UnitTest;

@Category(UnitTest.class)
public class HandshakeRequestTest {
  @Before
  public void setup() {
    
  }

  @Test
  public void testSerialization() throws IOException {
    String version = "0.1";
    String mechanism = "PLAIN";
    String correlationID = "a12345";
    String clientID = "client1";
    HandshakeRequest
        handshakeRequest =
        new HandshakeRequest(version, correlationID, clientID, mechanism);

    byte[] expectedByteArray = makeMessage(version, correlationID, clientID, mechanism);

    byte[] serializedRequest = handshakeRequest.toByteArray();

    assertArrayEquals(expectedByteArray, serializedRequest);
  }

  @Test
  public void testDeserialization() {
    makeMessage()
  }

  public byte[] makeMessage(String version, String correlationID, String clientID, String mechanism)
      throws IOException {
    byte[] versionBytes = version.getBytes(Charset.forName("UTF8"));
    byte[] versionLength = ByteBuffer.allocate(Integer.BYTES).putInt(versionBytes.length).array();
    byte[] mechanismBytes = mechanism.getBytes(Charset.forName("UTF8"));
    byte[] mechanismLength = ByteBuffer.allocate(Integer.BYTES).putInt(mechanismBytes.length).array();
    byte[] correlationBytes = correlationID.getBytes(Charset.forName("UTF8"));
    byte[] correlationLength = ByteBuffer.allocate(Integer.BYTES).putInt(correlationBytes.length).array();
    byte[] clientIDBytes = clientID.getBytes(Charset.forName("UTF8"));
    byte[] clientIDLength = ByteBuffer.allocate(Integer.BYTES).putInt(clientIDBytes.length).array();

    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream( );
    byteArrayStream.write(versionLength);
    byteArrayStream.write(versionBytes);
    byteArrayStream.write(correlationLength);
    byteArrayStream.write(correlationBytes);
    byteArrayStream.write(clientIDLength);
    byteArrayStream.write(clientIDBytes);
    byteArrayStream.write(mechanismLength);
    byteArrayStream.write(mechanismBytes);

    return byteArrayStream.toByteArray();
  }
}
