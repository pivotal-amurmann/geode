package org.apache.geode.protocol.protobuf.operations;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.protocol.protobuf.AuthenticationAPI;
import org.apache.geode.test.junit.categories.UnitTest;

@Category(UnitTest.class)
public class AuthenticationHandshakeRequestHandlerJUnitTest extends OperationHandlerJUnitTest {
  @Before
  public void setup() throws Exception {
    super.setUp();
  }

  @Test
  public void respondsWithAuthenticationHandshakeResponse() {
//    AuthenticationAPI.AuthenticationHandshakeRequest
//        handshakeRequest =
//        AuthenticationAPI.AuthenticationHandshakeRequest.newBuilder().addMechanism("PLAIN").build();

    // verify response
    // verify authenticationContext got called
  }
}
