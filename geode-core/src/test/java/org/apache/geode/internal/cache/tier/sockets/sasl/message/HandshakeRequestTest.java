package org.apache.geode.internal.cache.tier.sockets.sasl.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.security.sasl.AuthenticationException;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.test.junit.categories.UnitTest;

@Category(UnitTest.class)
public class HandshakeRequestTest {
  String version;
  String mechanism;
  String correlationID;
  String clientID;

  @Before
  public void setup() {
    version = "0.1";
    mechanism = "PLAIN";
    correlationID = "a12345";
    clientID = "client1";
  }

  @Test
  public void testSerialization() throws IOException {
    HandshakeRequest
        handshakeRequest =
        new HandshakeRequest(version, correlationID, clientID, mechanism);

    byte[] expectedByteArray = makeMessage(version, correlationID, clientID, mechanism);

    byte[] serializedRequest = handshakeRequest.toByteArray();

    assertArrayEquals(expectedByteArray, serializedRequest);
  }

  @Test
  public void testDeserialization() throws IOException {
    byte[] message = makeMessage(version, correlationID, clientID, mechanism);
    HandshakeRequest decodedMessage = HandshakeRequest.from(message);
    assertEquals(version, decodedMessage.getVersion());
    assertEquals(correlationID, decodedMessage.getCorrelationID());
    assertEquals(mechanism, decodedMessage.getMechanism());
    assertEquals(clientID, decodedMessage.getClientID());
  }

  @Test
  public void testInvalidVersion() throws IOException {
    String unhappyVersion = "99.4";
    byte[] message = makeMessage(unhappyVersion, correlationID, clientID, mechanism);
    Throwable throwable = catchThrowable(() -> HandshakeRequest.from(message));

    assertThat(throwable).isInstanceOf(AuthenticationException.class);
  }

  @Test
  public void testmalformedMessage() throws IOException {
    byte[] message = HandshakeRequest.VERSION.getBytes();
    Throwable throwable = catchThrowable(() -> HandshakeRequest.from(message));

    assertThat(throwable).isInstanceOf(AuthenticationException.class);
  }

  public byte[] makeMessage(String version, String correlationID, String clientID, String mechanism)
      throws IOException {
    byte[] versionBytes = version.getBytes(Charset.forName("UTF8"));
    byte[] mechanismBytes = mechanism.getBytes(Charset.forName("UTF8"));
    byte[] correlationBytes = correlationID.getBytes(Charset.forName("UTF8"));
    byte[] clientIDBytes = clientID.getBytes(Charset.forName("UTF8"));

    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
    byteArrayStream.write(versionBytes);
    byteArrayStream.write(new byte[]{'\u0000'});
    byteArrayStream.write(correlationBytes);
    byteArrayStream.write(new byte[]{'\u0000'});
    byteArrayStream.write(clientIDBytes);
    byteArrayStream.write(new byte[]{'\u0000'});
    byteArrayStream.write(mechanismBytes);
    byteArrayStream.write(new byte[]{'\u0000'});

    return byteArrayStream.toByteArray();
  }
}
