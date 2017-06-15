package org.apache.geode.client.protocol;

import com.google.protobuf.ByteString;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionService;
import org.apache.geode.protocol.OpsProcessor;
import org.apache.geode.protocol.operations.protobuf.GetRequestOperationHandler;
import org.apache.geode.protocol.operations.registry.OperationsHandlerRegistry;
import org.apache.geode.protocol.operations.registry.exception.OperationHandlerAlreadyRegisteredException;
import org.apache.geode.protocol.operations.registry.exception.OperationHandlerNotRegisteredException;
import org.apache.geode.protocol.protobuf.BasicTypes;
import org.apache.geode.protocol.protobuf.ClientProtocol;
import org.apache.geode.protocol.protobuf.RegionAPI;
import org.apache.geode.serialization.ProtobufSerializationServiceImpl;
import org.apache.geode.serialization.codec.StringCodec;
import org.apache.geode.serialization.protobuf.translation.EncodingTypeTranslator;
import org.apache.geode.serialization.protobuf.translation.exception.UnsupportedEncodingTypeException;
import org.apache.geode.serialization.registry.SerializationCodecRegistry;
import org.apache.geode.serialization.registry.exception.CodecAlreadyRegisteredForTypeException;
import org.apache.geode.serialization.registry.exception.CodecNotRegisteredForTypeException;
import org.apache.geode.test.dunit.Assert;
import org.apache.geode.test.junit.categories.UnitTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(UnitTest.class)
public class IntegrationTest {

  public static final String TEST_KEY = "my key";
  public static final String TEST_VALUE = "my value";
  public static final String TEST_REGION = "test region";

  @Test
  public void testFullRequestToCache() throws OperationHandlerAlreadyRegisteredException, CodecAlreadyRegisteredForTypeException, UnsupportedEncodingTypeException, CodecNotRegisteredForTypeException, OperationHandlerNotRegisteredException {
    OperationsHandlerRegistry operationsHandlerRegistry = new OperationsHandlerRegistry();
    OpsProcessor processor = new OpsProcessor(operationsHandlerRegistry, new ProtobufSerializationServiceImpl());
    SerializationCodecRegistry serializationCodecRegistry = new SerializationCodecRegistry();

    GetRequestOperationHandler operationHandler = (GetRequestOperationHandler) operationsHandlerRegistry.getOperationHandlerForOperationId(ClientProtocol.Request.RequestAPICase.GETREQUEST.getNumber());

    Region regionStub = mock(Region.class);
    when(regionStub.get(TEST_KEY)).thenReturn(TEST_VALUE);

    RegionService regionServiceStub = mock(RegionService.class);
    when(regionServiceStub.getRegion(TEST_REGION)).thenReturn(regionStub);

    operationHandler.setRegionService(regionServiceStub);

    EncodingTypeTranslator encodingTypeTranslator = new EncodingTypeTranslator();
    StringCodec stringCodec = (StringCodec) serializationCodecRegistry.getCodecForType(
      encodingTypeTranslator.getSerializationTypeForEncodingType(BasicTypes.EncodingType.STRING)
    );
    RegionAPI.GetRequest.Builder getRequestBuilder = RegionAPI.GetRequest.newBuilder();
    getRequestBuilder.
      setRegionName(TEST_REGION).
      setKey(
        BasicTypes.EncodedValue.newBuilder().
          setEncodingType(BasicTypes.EncodingType.STRING).
          setValue(ByteString.copyFrom(stringCodec.encode(TEST_KEY)))
      );
    ClientProtocol.Request request = ClientProtocol.Request.newBuilder()
      .setGetRequest(getRequestBuilder).build();

    ClientProtocol.Response response = processor.process(request);

    Assert.assertTrue(response.getGetResponse().hasResult());
    Assert.assertEquals(BasicTypes.EncodingType.STRING, response.getGetResponse().getResult().getEncodingType());
    String actualValue = stringCodec.decode(response.getGetResponse().getResult().getValue().toByteArray());
    Assert.assertEquals(TEST_VALUE, actualValue);
  }
}
