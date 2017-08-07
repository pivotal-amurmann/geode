package org.apache.geode.protocol.protobuf.registry;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.protocol.protobuf.ClientProtocol;
import org.apache.geode.test.junit.categories.UnitTest;

@Category(UnitTest.class)
public class OperationContextRegistryJUnitTest {
  @Test
  public void verifyOperationContextForRequestAPICasesAreComplete() {
    OperationContextRegistry operationContextRegistry = new OperationContextRegistry();
    List<ClientProtocol.Request.RequestAPICase>
        missingApiCases =
        Arrays.stream(ClientProtocol.Request.RequestAPICase.values()
        ).filter(apiCase ->
            operationContextRegistry.getOperationContext(apiCase) == null
        ).filter(apiCase -> apiCase != ClientProtocol.Request.RequestAPICase.REQUESTAPI_NOT_SET
        ).collect(Collectors.toList());
    assertEquals("missing API case(s) in registry: "+ missingApiCases, 0, missingApiCases.size());
  }
}