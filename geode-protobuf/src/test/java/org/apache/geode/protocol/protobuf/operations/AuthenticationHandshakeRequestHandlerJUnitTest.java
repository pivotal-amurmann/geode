package org.apache.geode.protocol.protobuf.operations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.protocol.protobuf.AuthenticationAPI;
import org.apache.geode.protocol.protobuf.BasicTypes;
import org.apache.geode.protocol.protobuf.Failure;
import org.apache.geode.protocol.protobuf.Result;
import org.apache.geode.protocol.protobuf.Success;
import org.apache.geode.test.junit.categories.UnitTest;

@Category(UnitTest.class)
public class AuthenticationHandshakeRequestHandlerJUnitTest extends OperationHandlerJUnitTest {
  @Before
  public void setup() throws Exception {
    super.setUp();

    operationHandler = new AuthenticationHandshakeRequestHandler();
  }

  @Test
  public void respondsWithAuthenticationHandshakeResponseContainingAgreedUponMechanism() {
    AuthenticationAPI.AuthenticationHandshakeRequest
        clientHandshakeRequest =
        AuthenticationAPI.AuthenticationHandshakeRequest.newBuilder().addMechanism("PLAIN").addMechanism("UnknownMechanismToServer").build();

    Success<AuthenticationAPI.AuthenticationHandshakeResponse> result =
        (Success<AuthenticationAPI.AuthenticationHandshakeResponse>) operationHandler
            .process(serializationServiceStub, clientHandshakeRequest, executionContext);

    assertEquals("PLAIN", result.getMessage().getMechanism());
  }

  @Test
  public void submitUnsupportedAuthenticationMechanism() {
    String unknown_mechanism = "UnknownMechanismToServer";
    AuthenticationAPI.AuthenticationHandshakeRequest
        handshakeRequest =
        AuthenticationAPI.AuthenticationHandshakeRequest.newBuilder().addMechanism(
            unknown_mechanism).build();

    Result result =
        operationHandler.process(serializationServiceStub, handshakeRequest, executionContext);

    assertThat(result, new IsInstanceOf(Failure.class));
    assertThat("Expected failure message due to unsupported mechanism submitted",
        result.getErrorMessage().getMessage(), new StringContains("No mutually agreed upon mechanism"));
  }
}
