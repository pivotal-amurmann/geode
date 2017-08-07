package org.apache.geode.protocol.protobuf.operations;

import org.apache.geode.internal.cache.tier.sockets.sasl.ExecutionContext;
import org.apache.geode.protocol.operations.OperationHandler;
import org.apache.geode.protocol.protobuf.AuthenticationAPI;
import org.apache.geode.protocol.protobuf.BasicTypes;
import org.apache.geode.protocol.protobuf.Failure;
import org.apache.geode.protocol.protobuf.Result;
import org.apache.geode.protocol.protobuf.Success;
import org.apache.geode.serialization.SerializationService;

public class AuthenticationHandshakeRequestHandler implements
    OperationHandler<AuthenticationAPI.AuthenticationHandshakeRequest, AuthenticationAPI.AuthenticationHandshakeResponse> {

  @Override
  public Result<AuthenticationAPI.AuthenticationHandshakeResponse> process(
      SerializationService serializationService,
      AuthenticationAPI.AuthenticationHandshakeRequest request, ExecutionContext executionContext) {
    if(request.getMechanismList().contains("PLAIN")) {
      return new Success<>(AuthenticationAPI.AuthenticationHandshakeResponse.newBuilder().setMechanism("PLAIN").build());
    } else {
      return Failure.of(BasicTypes.ErrorResponse.newBuilder().setMessage("No mutually agreed upon mechanism").build());
    }
  }
}
