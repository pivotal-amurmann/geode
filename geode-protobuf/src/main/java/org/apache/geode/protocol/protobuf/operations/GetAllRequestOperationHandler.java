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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GetAllRequestOperationHandler
    implements OperationHandler<ClientProtocol.Request, ClientProtocol.Response> {
  private static Logger logger = LogManager.getLogger();

  @Override
  public ClientProtocol.Response process(SerializationService serializationService,
      ClientProtocol.Request request, Cache cache) {
    if (request.getRequestAPICase() != ClientProtocol.Request.RequestAPICase.GETALLREQUEST) {
      return ProtobufResponseUtilities.createAndLogErrorResponse(false, false,
          "Improperly formatted getAll request message.", logger, null);
    }
    RegionAPI.GetAllRequest getAllRequest = request.getGetAllRequest();

    String regionName = getAllRequest.getRegionName();
    Region region = cache.getRegion(regionName);
    if (region == null) {
      return ProtobufResponseUtilities.createErrorResponse(false, false, "Region not found");
    }

    try {
      Set<Object> keys = new HashSet<>();
      for (BasicTypes.EncodedValue key : getAllRequest.getKeyList()) {
        keys.add(ProtobufUtilities.decodeValue(serializationService, key));
      }
      Map<Object, Object> results = region.getAll(keys);
      Set<BasicTypes.Entry> entries = new HashSet<>();
      for (Map.Entry<Object, Object> entry : results.entrySet()) {
        entries.add(
            ProtobufUtilities.createEntry(serializationService, entry.getKey(), entry.getValue()));
      }
      return ProtobufResponseUtilities.createGetAllResponse(entries);
    } catch (UnsupportedEncodingTypeException ex) {
      // can be thrown by encoding or decoding.
      return ProtobufResponseUtilities.createAndLogErrorResponse(false, false,
          "Encoding not supported.", logger, ex);
    } catch (CodecNotRegisteredForTypeException ex) {
      return ProtobufResponseUtilities.createAndLogErrorResponse(true, false,
          "Codec error in protobuf deserialization.", logger, ex);
    }
  }
}
