package org.apache.geode.protocol.operations.registry;

import org.apache.geode.protocol.operations.OperationContext;
import org.apache.geode.protocol.protobuf.ClientProtocol;
import org.apache.geode.protocol.protobuf.ClientProtocol.Request.RequestAPICase;
import org.apache.geode.protocol.protobuf.operations.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OperationContextRegistry {
  private Map<RequestAPICase, OperationContext> operationContexts = new ConcurrentHashMap<>();

  public OperationContextRegistry() {
    addContexts();
  }

  public OperationContext getOperationContext(RequestAPICase apiCase) {
    return operationContexts.get(apiCase);
  }

  private void addContexts() {
    operationContexts.put(RequestAPICase.GETREQUEST,
        new OperationContext<>(ClientProtocol.Request::getGetRequest,
            new GetRequestOperationHandler(),
            opsResp -> ClientProtocol.Response.newBuilder().setGetResponse(opsResp)));

    operationContexts.put(RequestAPICase.GETALLREQUEST,
        new OperationContext<>(ClientProtocol.Request::getGetAllRequest,
            new GetAllRequestOperationHandler(),
            opsResp -> ClientProtocol.Response.newBuilder().setGetAllResponse(opsResp)));

    operationContexts.put(RequestAPICase.PUTREQUEST,
        new OperationContext<>(ClientProtocol.Request::getPutRequest,
            new PutRequestOperationHandler(),
            opsResp -> ClientProtocol.Response.newBuilder().setPutResponse(opsResp)));

    operationContexts.put(RequestAPICase.PUTALLREQUEST,
        new OperationContext<>(ClientProtocol.Request::getPutAllRequest,
            new PutAllRequestOperationHandler(),
            opsResp -> ClientProtocol.Response.newBuilder().setPutAllResponse(opsResp)));

    operationContexts.put(RequestAPICase.REMOVEREQUEST,
        new OperationContext<>(ClientProtocol.Request::getRemoveRequest,
            new RemoveRequestOperationHandler(),
            opsResp -> ClientProtocol.Response.newBuilder().setRemoveResponse(opsResp)));

    operationContexts.put(RequestAPICase.GETREGIONNAMESREQUEST,
        new OperationContext<>(ClientProtocol.Request::getGetRegionNamesRequest,
            new GetRegionNamesRequestOperationHandler(),
            opsResp -> ClientProtocol.Response.newBuilder().setGetRegionNamesResponse(opsResp)));
  }
}
