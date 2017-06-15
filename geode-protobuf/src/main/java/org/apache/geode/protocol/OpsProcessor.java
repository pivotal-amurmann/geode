package org.apache.geode.protocol;

import org.apache.geode.protocol.operations.OperationHandler;
import org.apache.geode.protocol.operations.ProtobufRequestOperationParser;
import org.apache.geode.protocol.operations.registry.OperationsHandlerRegistry;
import org.apache.geode.protocol.operations.registry.exception.OperationHandlerNotRegisteredException;
import org.apache.geode.protocol.protobuf.ClientProtocol;
import org.apache.geode.protocol.protobuf.RegionAPI;
import org.apache.geode.serialization.SerializationService;

public class OpsProcessor {
    private final OperationsHandlerRegistry opsHandlerRegistry;
    private final SerializationService serializationService;

    public OpsProcessor(OperationsHandlerRegistry opsHandlerRegistry,
                        SerializationService serializationService) {
        this.opsHandlerRegistry = opsHandlerRegistry;
        this.serializationService = serializationService;
    }

    public ClientProtocol.Response process(ClientProtocol.Request request) {
        OperationHandler opsHandler = null;
        try {
            opsHandler = opsHandlerRegistry.getOperationHandlerForOperationId(request.getRequestAPICase().getNumber());
        } catch (OperationHandlerNotRegisteredException e) {
            e.printStackTrace();
        }

        Object responseMessage = opsHandler.process(serializationService,
                ProtobufRequestOperationParser.getRequestForOperationTypeID(request));
        return ClientProtocol.Response.newBuilder()
                .setGetResponse((RegionAPI.GetResponse) responseMessage).build();
    }
}