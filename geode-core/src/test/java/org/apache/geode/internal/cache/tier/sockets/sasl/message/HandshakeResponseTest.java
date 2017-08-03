package org.apache.geode.internal.cache.tier.sockets.sasl.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.security.sasl.AuthenticationException;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.test.junit.categories.UnitTest;

@Category(UnitTest.class)
public class HandshakeResponseTest {
  String mechanism;
  String correlationID;

  @Before
  public void setup() {
    mechanism = "PLAIN";
    correlationID = "a12345";
  }

  @Test
  public void testSerialization() throws IOException {
    HandshakeResponse handshake = new HandshakeResponse(correlationID, mechanism);
    byte[] actualMessage = handshake.toByteArray();
    byte[] expectedMessage = makeMessage(correlationID, mechanism);

    assertArrayEquals(expectedMessage, actualMessage);
  }

  @Test
  public void testDeserialization() throws IOException {
    byte[] serializedMessage = makeMessage(correlationID, mechanism);
    HandshakeResponse decodedResponse = HandshakeResponse.from(serializedMessage);

    assertEquals(mechanism, decodedResponse.getMechanism());
    assertEquals(correlationID, decodedResponse.getCorrelationId());
  }

  @Test
  public void testmalformedMessage() throws IOException {
    byte[] message = "Give me an error".getBytes();
    Throwable throwable = catchThrowable(() -> HandshakeResponse.from(message));

    assertThat(throwable).isInstanceOf(AuthenticationException.class);
  }

  public byte[] makeMessage(String correlationID, String mechanism)
      throws IOException {
    byte[] mechanismBytes = mechanism.getBytes(Charset.forName("UTF8"));
    byte[] correlationBytes = correlationID.getBytes(Charset.forName("UTF8"));

    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
    byteArrayStream.write(correlationBytes);
    byteArrayStream.write(new byte[]{'\u0000'});
    byteArrayStream.write(mechanismBytes);
    byteArrayStream.write(new byte[]{'\u0000'});

    return byteArrayStream.toByteArray();
  }
}