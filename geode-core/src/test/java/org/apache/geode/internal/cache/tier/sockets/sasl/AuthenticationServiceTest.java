package org.apache.geode.internal.cache.tier.sockets.sasl;

import static org.apache.geode.internal.cache.tier.sockets.sasl.AuthenticationService.AuthenticationProgress.AUTHENTICATION_IN_PROGRESS;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.internal.HeapDataOutputStream;
import org.apache.geode.internal.cache.tier.sockets.sasl.message.HandshakeRequest;
import org.apache.geode.internal.cache.tier.sockets.sasl.message.HandshakeResponse;
import org.apache.geode.test.junit.categories.UnitTest;

@Category(UnitTest.class)
public class AuthenticationServiceTest {
  @Test
  public void testHandshakeRequestInProgressAndResponds() throws IOException {
    AuthenticationService testService = new AuthenticationService();
    byte[] outputArray = new byte[1000];
    HeapDataOutputStream heapDataOutputStream = new HeapDataOutputStream(outputArray);
    InputStream inputStream = heapDataOutputStream.getInputStream();
    DataOutputStream outputStream = new DataOutputStream(heapDataOutputStream);

    HandshakeRequest
        handshakeRequest =
        new HandshakeRequest(HandshakeRequest.VERSION, "123a", "myID", "PLAIN");
    outputStream.write(handshakeRequest.toByteArray());

    AuthenticationService.AuthenticationProgress progressState = testService.process(inputStream, outputStream);

    assertEquals(AUTHENTICATION_IN_PROGRESS, progressState);

    byte[] responseBytes = heapDataOutputStream.toByteArray();
//    HandshakeResponse response = HandshakeResponse.from(responseBytes);
//    assertEquals("PLAIN", respone.getMechanism());
  }
}