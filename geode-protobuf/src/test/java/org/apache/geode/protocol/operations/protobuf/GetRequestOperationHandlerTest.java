package org.apache.geode.protocol.operations.protobuf;

import com.google.protobuf.ByteString;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionService;
import org.apache.geode.protocol.protobuf.BasicTypes;
import org.apache.geode.protocol.protobuf.ClientProtocol;
import org.apache.geode.protocol.protobuf.RegionAPI;
import org.apache.geode.serialization.SerializationService;
import org.apache.geode.serialization.codec.StringCodec;
import org.apache.geode.serialization.exception.SerializationServiceException;
import org.apache.geode.serialization.protobuf.translation.EncodingTypeTranslator;
import org.apache.geode.serialization.protobuf.translation.exception.UnsupportedEncodingTypeException;
import org.apache.geode.serialization.registry.SerializationCodecRegistry;
import org.apache.geode.serialization.registry.exception.CodecAlreadyRegisteredForTypeException;
import org.apache.geode.serialization.registry.exception.CodecNotRegisteredForTypeException;
import org.apache.geode.test.dunit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetRequestOperationHandlerTest {
  public static final String TEST_KEY = "my key";
  public static final String TEST_VALUE = "my value";
  public static final String TEST_REGION = "test region";
  public RegionService regionServiceStub;
  public SerializationService serializationServiceStub;

  @Before
  public void setUp() throws Exception {
    serializationServiceStub = mock(SerializationService.class);
    when(serializationServiceStub.decode(BasicTypes.EncodingType.STRING, TEST_KEY.getBytes(Charset.forName("UTF-8")))).thenReturn(TEST_KEY);
    when(serializationServiceStub.encode(BasicTypes.EncodingType.STRING, TEST_VALUE)).thenReturn(TEST_VALUE.getBytes(Charset.forName("UTF-8")));

    Region regionStub = mock(Region.class);
    when(regionStub.get(TEST_KEY)).thenReturn(TEST_VALUE);

    regionServiceStub = mock(RegionService.class);
    when(regionServiceStub.getRegion(TEST_REGION)).thenReturn(regionStub);
  }

  @Test
  public void processReturnsTheEncodedValueFromTheRegion() throws UnsupportedEncodingTypeException, SerializationServiceException, CodecNotRegisteredForTypeException, CodecAlreadyRegisteredForTypeException {
    GetRequestOperationHandler operationHandler = new GetRequestOperationHandler();
    operationHandler.setRegionService(regionServiceStub);

    RegionAPI.GetResponse response = operationHandler.process(serializationServiceStub, makeGetRequest());

    Assert.assertEquals(BasicTypes.EncodingType.STRING, response.getResult().getEncodingType());
    String actualValue = getStringCodec().decode(response.getResult().getValue().toByteArray());
    Assert.assertEquals(TEST_VALUE, actualValue);
  }

  private RegionAPI.GetRequest makeGetRequest() throws CodecNotRegisteredForTypeException, UnsupportedEncodingTypeException, CodecAlreadyRegisteredForTypeException {
    StringCodec stringCodec = getStringCodec();
    RegionAPI.GetRequest.Builder getRequestBuilder = RegionAPI.GetRequest.newBuilder();
    getRequestBuilder.
      setRegionName(TEST_REGION).
      setKey(
        BasicTypes.EncodedValue.newBuilder().
          setEncodingType(BasicTypes.EncodingType.STRING).
          setValue(ByteString.copyFrom(stringCodec.encode(TEST_KEY)))
      );

    return getRequestBuilder.build();
  }

  private StringCodec getStringCodec() throws CodecAlreadyRegisteredForTypeException, CodecNotRegisteredForTypeException, UnsupportedEncodingTypeException {
    EncodingTypeTranslator encodingTypeTranslator = new EncodingTypeTranslator();
    SerializationCodecRegistry serializationCodecRegistry = new SerializationCodecRegistry();
    return (StringCodec) serializationCodecRegistry.getCodecForType(
      encodingTypeTranslator.getSerializationTypeForEncodingType(BasicTypes.EncodingType.STRING)
    );
  }
}