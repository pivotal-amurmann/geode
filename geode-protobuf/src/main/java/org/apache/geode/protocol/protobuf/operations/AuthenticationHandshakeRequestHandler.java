package org.apache.geode.protocol.protobuf.operations;

import org.apache.geode.internal.cache.tier.sockets.sasl.ExecutionContext;
import org.apache.geode.protocol.operations.OperationHandler;
import org.apache.geode.protocol.protobuf.AuthenticationAPI;
import org.apache.geode.protocol.protobuf.Result;
import org.apache.geode.serialization.SerializationService;

public class AuthenticationHandshakeRequestHandler implements
    OperationHandler<AuthenticationAPI.AuthenticationHandshakeRequest, AuthenticationAPI.AuthenticationHandshakeResponse> {

  @Override
  public Result<AuthenticationAPI.AuthenticationHandshakeResponse> process(
      SerializationService serializationService,
      AuthenticationAPI.AuthenticationHandshakeRequest request, ExecutionContext executionContext) {
    return null;
  }
}
