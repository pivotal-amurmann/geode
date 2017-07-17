package org.apache.geode.protocol.operations;

import org.apache.geode.protocol.protobuf.ClientProtocol;
import org.apache.geode.protocol.protobuf.ProtobufOpsProcessor;

import java.util.function.Function;

public class OperationContext<OperationReq, OperationResponse> {
  private OperationHandler<OperationReq, OperationResponse> operationHandler;
  private Function<ClientProtocol.Request, OperationReq> fromRequest;
  private Function<OperationResponse, ClientProtocol.Response.Builder> toResponse;
  private Function<ClientProtocol.ErrorResponse, ClientProtocol.Response.Builder> toErrorResponse;

  public OperationContext(Function<ClientProtocol.Request, OperationReq> fromRequest,
      OperationHandler<OperationReq, OperationResponse> operationHandler,
      Function<OperationResponse, ClientProtocol.Response.Builder> toResponse) {
    this.operationHandler = operationHandler;
    this.fromRequest = fromRequest;
    this.toResponse = toResponse;
    this.toErrorResponse = OperationContext::makeErrorBuilder;
  }

  public static ClientProtocol.Response.Builder makeErrorBuilder(
      ClientProtocol.ErrorResponse errorResponse) {
    return ClientProtocol.Response.newBuilder().setErrorResponse(errorResponse);
  }

  public OperationHandler<OperationReq, OperationResponse> getOperationHandler() {
    return operationHandler;
  }

  public Function<ClientProtocol.Request, OperationReq> getFromRequest() {
    return fromRequest;
  }

  public Function<OperationResponse, ClientProtocol.Response.Builder> getToResponse() {
    return toResponse;
  }

  public Function<ClientProtocol.ErrorResponse, ClientProtocol.Response.Builder> getToErrorResponse() {
    return toErrorResponse;
  }
}
