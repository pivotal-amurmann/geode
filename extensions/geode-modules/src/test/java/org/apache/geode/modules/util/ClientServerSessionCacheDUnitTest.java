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
package org.apache.geode.modules.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.apache.juli.logging.Log;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.Scope;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.execute.FunctionException;
import org.apache.geode.cache.execute.FunctionService;
import org.apache.geode.cache.server.CacheServer;
import org.apache.geode.distributed.internal.DM;
import org.apache.geode.distributed.internal.MembershipListener;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.modules.session.catalina.ClientServerSessionCache;
import org.apache.geode.modules.session.catalina.SessionManager;
import org.apache.geode.test.dunit.DistributedTestUtils;
import org.apache.geode.test.dunit.Host;
import org.apache.geode.test.dunit.VM;
import org.apache.geode.test.dunit.cache.internal.JUnit4CacheTestCase;
import org.apache.geode.test.junit.categories.DistributedTest;

@Category(DistributedTest.class)
public class ClientServerSessionCacheDUnitTest extends JUnit4CacheTestCase {

  public static final String SESSION_REGION_NAME = RegionHelper.NAME + "_sessions";

  private Host host;
  private VM server0;
  private VM server1;
  private VM client;

  @Override
  public final void postSetUp() throws Exception {
    host = Host.getHost(0);
    server0 = host.getVM(0);
    server1 = host.getVM(1);
    client = host.getVM(2);
  }

  @Test
  public void multipleGeodeServersCreateSessionRegion() {
    server0.invoke(this::startCacheServer);
    server1.invoke(this::startCacheServer);

    client.invoke(this::startClientSessionCache);

    server0.invoke(this::validateServer);
    server1.invoke(this::validateServer);
  }

  @Test
  public void addServerToExistingClusterCreatesSessionRegion() {
    server0.invoke(this::startCacheServer);

    client.invoke(this::startClientSessionCache);

    server0.invoke(this::validateServer);

    server1.invoke(this::startCacheServer);

    // Session region may be created asynchronously on the second server
    server1
        .invoke(() -> Awaitility.waitAtMost(5 * 60, TimeUnit.SECONDS).until(this::validateServer));
  }

  @Test
  public void startingAClientWithoutServersFails() {
    assertThatThrownBy(() -> client.invoke(this::startClientSessionCache))
        .hasCauseInstanceOf(FunctionException.class);
  }

  @Test
  public void canPrecreateSessionRegionBeforeStartingClient() {
    server0.invoke(this::startCacheServer);
    server1.invoke(this::startCacheServer);

    server0.invoke(this::createSessionRegion);
    server1.invoke(this::createSessionRegion);

    client.invoke(this::startClientSessionCache);

    server0.invoke(this::validateServer);
    server1.invoke(this::validateServer);
  }

  @Test
  public void precreatedRegionIsNotCopiedToNewlyStartedServers() {
    server0.invoke(this::startCacheServer);

    server0.invoke(this::createSessionRegion);

    client.invoke(this::startClientSessionCache);
    server1.invoke(this::startCacheServer);

    server1.invoke(
        () -> Awaitility.waitAtMost(5 * 60, TimeUnit.SECONDS).until(this::validateBootstrapped));

    // server1 should not have created the session region
    // If the user precreated the region, they must manually
    // create it on all servers
    server1.invoke(() -> {
      Region<Object, Object> region = getCache().getRegion(SESSION_REGION_NAME);
      assertThat(region).isNull();
    });

  }

  @Test
  public void cantPrecreateMismatchedSessionRegionBeforeStartingClient() {
    server0.invoke(this::startCacheServer);
    server1.invoke(this::startCacheServer);

    server0.invoke(this::createMismatchedSessionRegion);
    server1.invoke(this::createMismatchedSessionRegion);

    assertThatThrownBy(() -> client.invoke(this::startClientSessionCache))
        .hasCauseInstanceOf(IllegalStateException.class);
  }

  private void createSessionRegion() {
    Cache cache = getCache();

    Region region =
        cache.<String, HttpSession>createRegionFactory(RegionShortcut.PARTITION_REDUNDANT)
            .setCustomEntryIdleTimeout(new SessionCustomExpiry()).create(SESSION_REGION_NAME);
  }

  private void createMismatchedSessionRegion() {
    Cache cache = getCache();

    Region region = cache.<String, HttpSession>createRegionFactory(RegionShortcut.PARTITION)
        .setCustomEntryIdleTimeout(new SessionCustomExpiry()).create(SESSION_REGION_NAME);
  }

  private void validateSessionRegion() {
    final InternalCache cache = getCache();

    final Region region = cache.getRegion(SESSION_REGION_NAME);
    assertThat(region).isNotNull();

    final RegionAttributes<Object, Object> expectedAttributes =
        cache.getRegionAttributes(RegionShortcut.PARTITION_REDUNDANT.toString());

    final RegionAttributes attributes = region.getAttributes();
    assertThat(attributes.getScope()).isEqualTo(expectedAttributes.getScope());
    assertThat(attributes.getDataPolicy()).isEqualTo(expectedAttributes.getDataPolicy());
    assertThat(attributes.getPartitionAttributes())
        .isEqualTo(expectedAttributes.getPartitionAttributes());
    assertThat(attributes.getCustomEntryIdleTimeout()).isInstanceOf(SessionCustomExpiry.class);
  }

  private void validateServer() {
    validateBootstrapped();
    validateSessionRegion();
  }

  private void validateBootstrapped() {
    final InternalCache cache = getCache();

    final DM distributionManager = cache.getInternalDistributedSystem().getDistributionManager();
    final Collection<MembershipListener> listeners = distributionManager.getMembershipListeners();
    assertThat(listeners).filteredOn(listener -> listener instanceof BootstrappingFunction)
        .hasSize(1);
    assertThat(FunctionService.getFunction(CreateRegionFunction.ID))
        .isInstanceOf(CreateRegionFunction.class);
    assertThat(FunctionService.getFunction(TouchPartitionedRegionEntriesFunction.ID))
        .isInstanceOf(TouchPartitionedRegionEntriesFunction.class);
    assertThat(FunctionService.getFunction(TouchReplicatedRegionEntriesFunction.ID))
        .isInstanceOf(TouchReplicatedRegionEntriesFunction.class);
    assertThat(FunctionService.getFunction(RegionSizeFunction.ID))
        .isInstanceOf(RegionSizeFunction.class);

    final Region<String, RegionConfiguration> region =
        cache.getRegion(CreateRegionFunction.REGION_CONFIGURATION_METADATA_REGION);
    assertThat(region).isNotNull();

    final RegionAttributes<String, RegionConfiguration> attributes = region.getAttributes();
    assertThat(attributes.getDataPolicy()).isEqualTo(DataPolicy.REPLICATE);
    assertThat(attributes.getScope()).isEqualTo(Scope.DISTRIBUTED_ACK);
    assertThat(attributes.getDataPolicy()).isEqualTo(DataPolicy.REPLICATE);
    assertThat(attributes.getCacheListeners())
        .filteredOn(listener -> listener instanceof RegionConfigurationCacheListener).hasSize(1);
  }

  private void startClientSessionCache() {
    final SessionManager sessionManager = mock(SessionManager.class);
    final Log logger = mock(Log.class);
    when(sessionManager.getLogger()).thenReturn(logger);
    when(sessionManager.getRegionName()).thenReturn(RegionHelper.NAME + "_sessions");
    when(sessionManager.getRegionAttributesId())
        .thenReturn(RegionShortcut.PARTITION_REDUNDANT.toString());

    final ClientCacheFactory clientCacheFactory = new ClientCacheFactory();
    clientCacheFactory.addPoolLocator("localhost", DistributedTestUtils.getLocatorPort());
    clientCacheFactory.setPoolSubscriptionEnabled(true);

    final ClientCache clientCache = getClientCache(clientCacheFactory);
    new ClientServerSessionCache(sessionManager, clientCache).initialize();
  }

  private void startCacheServer() throws IOException {
    final Cache cache = getCache();
    final CacheServer cacheServer = cache.addCacheServer();
    cacheServer.setPort(0);
    cacheServer.start();
  }
}
