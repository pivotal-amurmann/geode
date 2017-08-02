package org.apache.geode.internal.cache.tier.sockets.sasl;

import java.io.InputStream;
import java.io.OutputStream;

public class AuthenticationService {
  public enum AuthenticationProgress {
    AUTHENTICATION_IN_PROGRESS,
    AUTHENTICATION_COMPLETE,
    AUTHENTICATION_FAILED
  }

  public AuthenticationProgress process(InputStream inputStream, OutputStream outputStream) {

    return null;
  }
}
