package org.apache.geode.internal.cache.tier.sockets.sasl;

import org.apache.geode.cache.Cache;

public class ExecutionContext {
  private final Cache cache;
  private final AuthenticationContext authenticationContext;

  public ExecutionContext(Cache cache,
                          AuthenticationContext authenticationContext) {
    this.cache = cache;
    this.authenticationContext = authenticationContext;
  }

  public Cache getCache() {
    return cache;
  }

  public AuthenticationContext getAuthenticationContext() {
    return authenticationContext;
  }
}
