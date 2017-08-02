package org.apache.geode.internal.cache.tier.sockets.sasl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Properties;

import javax.security.sasl.AuthenticationException;
import javax.security.sasl.SaslException;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

import org.apache.geode.security.AuthenticationFailedException;
import org.apache.geode.security.SecurityManager;
import org.apache.geode.test.junit.categories.UnitTest;

@Category(UnitTest.class)
public class SaslPlainServerTest {

  private SecurityManager securityManagerMock;
  private SaslPlainServer saslPlainServer;
  private String authenticationID;
  private String username;
  private String password;

  @Before
  public void setup() {
    authenticationID = "99";
    username = "myName";
    password = "myPassword";

    securityManagerMock = mock(SecurityManager.class);
    saslPlainServer = new SaslPlainServer(securityManagerMock);
  }

  @Test
  public void testEvaluateResponseDelegatesToSecurityManager() throws SaslException {
    ArgumentCaptor<Properties>
        securityManagerAuthenticateCaptor =
        ArgumentCaptor.forClass(Properties.class);
    byte[] response = makeResponse(authenticationID, username, password);
    Object principal = mock(Object.class);
    when(securityManagerMock.authenticate(any(Properties.class))).thenReturn(principal);

    saslPlainServer.evaluateResponse(response);

    verify(securityManagerMock).authenticate(securityManagerAuthenticateCaptor.capture());
    List<Properties> passedProperties = securityManagerAuthenticateCaptor.getAllValues();
    assertEquals(1, passedProperties.size());
    assertEquals(username, passedProperties.get(0).getProperty("username"));
    assertEquals(password, passedProperties.get(0).getProperty("password"));

    assertTrue(saslPlainServer.isComplete());

    assertEquals(principal, saslPlainServer.getPrincipal());
  }

  @Test
  public void testEvaluateResponseFailsAuthenticationWhenSecurityManagerRejectsCredentials()
      throws SaslException {
    byte[] response = makeResponse(authenticationID, username, password);
    String errorMessage = "authentication failed";
    when(securityManagerMock.authenticate(any(Properties.class)))
        .thenThrow(new AuthenticationFailedException(
            errorMessage));

    Throwable throwable = catchThrowable(() -> saslPlainServer.evaluateResponse(response));

    assertThat(throwable).isInstanceOf(AuthenticationException.class).hasMessage(errorMessage);
    assertFalse(saslPlainServer.isComplete());
  }

  @Test
  public void testEvaluateResponseIfTheResponseIsMalformedItFailsAuthentication()
      throws SaslException {
    String errorMessage = "Unable to process authentication credentials.";
    byte[] response = (authenticationID + "\u0000" + username).getBytes();
    Throwable throwable = catchThrowable(() -> saslPlainServer.evaluateResponse(response));

    assertThat(throwable).isInstanceOf(AuthenticationException.class).hasMessage(errorMessage);
    assertFalse(saslPlainServer.isComplete());
  }

  private byte[] makeResponse(String authenticationID, String username, String password) {
    byte[] response = (authenticationID + "\u0000" + username + "\u0000" + password).getBytes();
    return response;
  }
}
