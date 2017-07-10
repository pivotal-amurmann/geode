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
package org.apache.geode.protocol.protobuf.operations;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.protocol.operations.OperationHandler;
import org.apache.geode.protocol.protobuf.BasicTypes;
import org.apache.geode.protocol.protobuf.ClientProtocol;
import org.apache.geode.protocol.protobuf.RegionAPI;
import org.apache.geode.protocol.protobuf.utilities.ProtobufResponseUtilities;
import org.apache.geode.protocol.protobuf.utilities.ProtobufUtilities;
import org.apache.geode.serialization.SerializationService;
import org.apache.geode.serialization.exception.UnsupportedEncodingTypeException;
import org.apache.geode.serialization.registry.exception.CodecNotRegisteredForTypeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PutAllRequestOperationHandler
    implements OperationHandler<ClientProtocol.Request, ClientProtocol.Response> {
  private static Logger logger = LogManager.getLogger();

  @Override
  public ClientProtocol.Response process(SerializationService serializationService,
      ClientProtocol.Request request, Cache cache) {
    RegionAPI.PutAllRequest putAllRequest = request.getPutAllRequest();
    String regionName = putAllRequest.getRegionName();
    Region region = cache.getRegion(regionName);

    Map<Object, Object> entries = new HashMap();
    ClientProtocol.Response errorResponse = valdiateAndPopulateEntries(serializationService, request, putAllRequest, regionName, region, entries);
    if(errorResponse != null) {
      return errorResponse;
    }

    Set<BasicTypes.EncodedValue> invalidKeys = putValues(serializationService, region, entries);

    return ProtobufResponseUtilities.createPutAllResponse(invalidKeys);
  }

  // Read all of the entries out of the protobuf and return an error (without performing any puts)
  // if any of the entries can't be decoded
  private  ClientProtocol.Response valdiateAndPopulateEntries(SerializationService serializationService, ClientProtocol.Request request, RegionAPI.PutAllRequest putAllRequest, String regionName, Region region,
                                                              Map<Object, Object> entries) {
    if (request.getRequestAPICase() != ClientProtocol.Request.RequestAPICase.PUTALLREQUEST) {
      return ProtobufResponseUtilities.createAndLogErrorResponse(false, false,
        "Improperly formatted put request message.", logger, null);
    }

    if (region == null) {
      return ProtobufResponseUtilities.createAndLogErrorResponse(false, false,
        "Region passed by client did not exist: " + regionName,
        logger, null);
    }

    try {
      for (BasicTypes.Entry entry : putAllRequest.getEntryList()) {
        Object decodedValue = ProtobufUtilities.decodeValue(serializationService, entry.getValue());
        Object decodedKey = ProtobufUtilities.decodeValue(serializationService, entry.getKey());

        entries.put(decodedKey, decodedValue);
      }
    } catch (UnsupportedEncodingTypeException ex) {
      return ProtobufResponseUtilities.createAndLogErrorResponse(false, false,
        "Encoding not supported ", logger, ex);
    } catch (CodecNotRegisteredForTypeException ex) {
      return ProtobufResponseUtilities.createAndLogErrorResponse(true, false,
        "Codec error in protobuf deserialization ", logger, ex);
    }

    return null;
  }

  private Set<BasicTypes.EncodedValue> putValues(SerializationService serializationService, Region region, Map<Object, Object> entries) {
    Set<BasicTypes.EncodedValue> invalidKeys = new HashSet<>();
    for (Map.Entry<Object, Object> entry : entries.entrySet()) {
      try {
        region.put(entry.getKey(), entry.getValue());
      } catch (ClassCastException ex) {
        try {
          // Shouldn't be possible for this to fail as we're encoding a value we previously decoded.
          // If this does throw, we don't have a proper response to the client (we can't encode the
          // failed key and we can't fail the operation if some keys succeeded), hence the severity
          // of the logger.fatal.
          invalidKeys
              .add(ProtobufUtilities.createEncodedValue(serializationService, entry.getKey()));
        } catch (UnsupportedEncodingTypeException e) {
          logger
              .fatal("Failed to create encoding for successfully decoded value: " + entry.getKey());
        } catch (CodecNotRegisteredForTypeException e) {
          logger
              .fatal("Failed to create encoding for successfully decoded value: " + entry.getKey());
        }
      }
    }
    return invalidKeys;
  }
}
