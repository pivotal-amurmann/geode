package org.apache.geode.internal.cache.tier.sockets.sasl;

import java.security.Principal;
import java.util.Properties;

import javax.security.sasl.AuthenticationException;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;


import org.apache.geode.security.AuthenticationFailedException;
import org.apache.geode.security.SecurityManager;

public class SaslPlainServer implements SaslServer{
  SecurityManager securityManager;
  boolean completed = false;
  private Object principal;

  public SaslPlainServer(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  public Object getPrincipal() {
    return principal;
  }

  @Override
  public String getMechanismName() {
    return null;
  }

  @Override
  public byte[] evaluateResponse(byte[] response) throws SaslException {
    Properties properties = new Properties();
    String[] susbStrings = new String(response).split("\u0000");
    try {
      properties.setProperty("authorizationID", susbStrings[0]);
      properties.setProperty("username", susbStrings[1]);
      properties.setProperty("password", susbStrings[2]);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new AuthenticationException("Unable to process authentication credentials.");
    }

    try {
      principal = securityManager.authenticate(properties);
    } catch (AuthenticationFailedException e) {
      throw new AuthenticationException(e.getMessage());
    }
    completed = true;
    return new byte[0];
  }

  @Override
  public boolean isComplete() {
    return completed;
  }

  @Override
  public String getAuthorizationID() {
    return null;
  }

  @Override
  public byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException {
    return new byte[0];
  }

  @Override
  public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException {
    return new byte[0];
  }

  @Override
  public Object getNegotiatedProperty(String propName) {
    return null;
  }

  @Override
  public void dispose() throws SaslException {

  }
}
