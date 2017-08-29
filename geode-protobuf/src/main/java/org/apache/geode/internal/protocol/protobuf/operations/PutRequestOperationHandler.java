/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal.protocol.protobuf.operations;

import org.apache.geode.annotations.Experimental;
import org.apache.geode.cache.Region;
import org.apache.geode.internal.cache.tier.sockets.MessageExecutionContext;
import org.apache.geode.internal.cache.tier.sockets.InvalidExecutionContextException;
import org.apache.geode.internal.protocol.operations.OperationHandler;
import org.apache.geode.internal.protocol.protobuf.BasicTypes;
import org.apache.geode.internal.protocol.protobuf.Failure;
import org.apache.geode.internal.protocol.protobuf.ProtocolErrorCode;
import org.apache.geode.internal.protocol.protobuf.RegionAPI;
import org.apache.geode.internal.protocol.protobuf.Result;
import org.apache.geode.internal.protocol.protobuf.Success;
import org.apache.geode.internal.protocol.protobuf.utilities.ProtobufResponseUtilities;
import org.apache.geode.internal.protocol.protobuf.utilities.ProtobufUtilities;
import org.apache.geode.internal.serialization.SerializationService;
import org.apache.geode.internal.serialization.exception.UnsupportedEncodingTypeException;
import org.apache.geode.internal.serialization.registry.exception.CodecNotRegisteredForTypeException;

@Experimental
public class PutRequestOperationHandler
    implements OperationHandler<RegionAPI.PutRequest, RegionAPI.PutResponse> {

  @Override
  public Result<RegionAPI.PutResponse> process(SerializationService serializationService,
      RegionAPI.PutRequest request, MessageExecutionContext executionContext)
      throws InvalidExecutionContextException {
    String regionName = request.getRegionName();
    Region region = executionContext.getCache().getRegion(regionName);
    if (region == null) {
      return Failure.of(
          ProtobufResponseUtilities.makeErrorResponse(ProtocolErrorCode.REGION_NOT_FOUND.codeValue,
              "Region passed by client did not exist: " + regionName));
    }

    try {
      BasicTypes.Entry entry = request.getEntry();

      Object decodedValue = ProtobufUtilities.decodeValue(serializationService, entry.getValue());
      Object decodedKey = ProtobufUtilities.decodeValue(serializationService, entry.getKey());
      try {
        region.put(decodedKey, decodedValue);
        return Success.of(RegionAPI.PutResponse.newBuilder().build());
      } catch (ClassCastException ex) {
        return Failure.of(ProtobufResponseUtilities.makeErrorResponse(
            ProtocolErrorCode.CONSTRAINT_VIOLATION.codeValue,
            "invalid key or value type for region " + regionName));
      }
    } catch (UnsupportedEncodingTypeException | CodecNotRegisteredForTypeException ex) {
      return Failure.of(ProtobufResponseUtilities
          .makeErrorResponse(ProtocolErrorCode.VALUE_ENCODING_ERROR.codeValue, ex.getMessage()));
    }
  }
}
