package org.apache.geode.serialization;

import org.apache.geode.protocol.protobuf.BasicTypes;
import org.apache.geode.serialization.exception.SerializationServiceException;
import org.apache.geode.serialization.registry.exception.CodecAlreadyRegisteredForTypeException;
import org.apache.geode.test.junit.categories.UnitTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(UnitTest.class)
public class ProtobufSerializationServiceImplTest {

  public static final String PAYLOAD = "my value";

  @Test
  public void valuesPreservedByEncodingThenDecoding() throws CodecAlreadyRegisteredForTypeException, SerializationServiceException {
    ProtobufSerializationServiceImpl protobufSerializationService = new ProtobufSerializationServiceImpl();

    testEncodeDecode(protobufSerializationService, BasicTypes.EncodingType.STRING, "testString");
    testEncodeDecode(protobufSerializationService, BasicTypes.EncodingType.FLOAT, (float)34.23);
    testEncodeDecode(protobufSerializationService, BasicTypes.EncodingType.DOUBLE, 34.23);
    testEncodeDecode(protobufSerializationService, BasicTypes.EncodingType.INT, 45);
    testEncodeDecode(protobufSerializationService, BasicTypes.EncodingType.SHORT, (short)45);
    testEncodeDecode(protobufSerializationService, BasicTypes.EncodingType.BYTE, (byte)45);
    testEncodeDecode(protobufSerializationService, BasicTypes.EncodingType.LONG, (long)45);
    testEncodeDecode(protobufSerializationService, BasicTypes.EncodingType.BOOLEAN, false);
    testEncodeDecode(protobufSerializationService, BasicTypes.EncodingType.BOOLEAN, true);
    testEncodeDecode(protobufSerializationService, BasicTypes.EncodingType.BINARY, "testString".getBytes());
    // TODO: Test JSON conversion
  }

  private void testEncodeDecode(ProtobufSerializationServiceImpl service, BasicTypes.EncodingType encodingType, Object data) throws SerializationServiceException {
    byte[] encodedValue = service.encode(encodingType, data);
    Object decdoedValue = service.decode(encodingType, encodedValue);
    Assert.assertEquals(data, decdoedValue);
  }
}