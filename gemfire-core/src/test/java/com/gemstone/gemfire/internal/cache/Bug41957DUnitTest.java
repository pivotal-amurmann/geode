/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal.cache;

import com.gemstone.gemfire.cache.*;
import com.gemstone.gemfire.distributed.internal.DistributionConfig;
import com.gemstone.gemfire.internal.AvailablePort;
import com.gemstone.gemfire.internal.cache.PartitionedRegion;

import com.gemstone.gemfire.cache30.ClientServerTestCase;
import com.gemstone.gemfire.cache30.CacheSerializableRunnable;

import com.gemstone.gemfire.cache.client.*;

import dunit.*;

import java.util.*;

/**
 * Test for bug 41957.
 * Basic idea is to have a client with a region with a low eviction limit
 * do a register interest with key&values and see if we end up with more entries
 * in the client than the eviction limit.
 *
 * @author darrel
 * @since 6.5
 */
 public class Bug41957DUnitTest extends ClientServerTestCase {

  public Bug41957DUnitTest(String name) {
    super(name);
  }

  public void tearDown2() throws Exception {
    super.tearDown2();
    disconnectAllFromDS();
  }

  public void testBug41957() {
    final Host host = Host.getHost(0);
    final VM server = host.getVM(0);
    final VM client = host.getVM(1);
    final String regionName = getUniqueName();
    final int mcastPort = AvailablePort.getRandomAvailablePort(AvailablePort.JGROUPS);
    final int serverPort = AvailablePort.getRandomAvailablePort(AvailablePort.SOCKET);
    final String serverHost = getServerHostName(server.getHost());

    createBridgeServer(server, mcastPort, regionName, serverPort, false);

    createBridgeClient(client, regionName, serverHost, new int[] {serverPort});

    client.invoke(new CacheSerializableRunnable("register interest") {
      public void run2() throws CacheException {
        Region region = getRootRegion(regionName);
        int ENTRIES_ON_SERVER = 10;
        for (int i=1; i <= ENTRIES_ON_SERVER; i++) {
          region.registerInterest("k"+i, InterestResultPolicy.KEYS_VALUES);
        }
        assertEquals(2, region.size());
      }
    });

    stopBridgeServer(server);
  }

  private void createBridgeServer(VM server, final int mcastPort, final String regionName, final int serverPort, final boolean createPR) {
    server.invoke(new CacheSerializableRunnable("Create server") {
      public void run2() throws CacheException {
        // Create DS
        Properties config = new Properties();
        config.setProperty(DistributionConfig.MCAST_PORT_NAME, String.valueOf(mcastPort));
        config.setProperty(DistributionConfig.LOCATORS_NAME, "");
        getSystem(config);

        // Create Region
        AttributesFactory factory = new AttributesFactory();
        factory.setCacheLoader(new CacheServerCacheLoader());
        if (createPR) {
          factory.setDataPolicy(DataPolicy.PARTITION);
          factory.setPartitionAttributes((new PartitionAttributesFactory()).create());
        } else {
          factory.setScope(Scope.DISTRIBUTED_ACK);
          factory.setDataPolicy(DataPolicy.REPLICATE);
        }
        Region region = createRootRegion(regionName, factory.create());
        if (createPR) {
          assertTrue(region instanceof PartitionedRegion);
        }
        int ENTRIES_ON_SERVER = 10;
        for (int i=1; i <= ENTRIES_ON_SERVER; i++) {
          region.create("k"+i, "v"+i);
        }
        try {
          startBridgeServer(serverPort);
        } catch (Exception e) {
          fail("While starting CacheServer", e);
        }
      }
    });
  }

  private void createBridgeClient(VM client, final String regionName, final String serverHost, final int[] serverPorts) {
    client.invoke(new CacheSerializableRunnable("Create client") {
      public void run2() throws CacheException {
        // Create DS
        Properties config = new Properties();
        config.setProperty(DistributionConfig.MCAST_PORT_NAME, "0");
        config.setProperty(DistributionConfig.LOCATORS_NAME, "");
        getSystem(config);

        // Create Region
        AttributesFactory factory = new AttributesFactory();
        factory.setScope(Scope.LOCAL);
        {
          PoolFactory pf = PoolManager.createFactory();
          pf.setSubscriptionEnabled(true);
          for (int i=0; i < serverPorts.length; i++) {
            pf.addServer(serverHost, serverPorts[i]);
          }
          pf.create("myPool");
        }
        int ENTRIES_ON_CLIENT = 2;
        factory.setPoolName("myPool");
        factory.setSubscriptionAttributes(new SubscriptionAttributes(InterestPolicy.ALL));
        factory.setEvictionAttributes(EvictionAttributes.createLRUEntryAttributes(ENTRIES_ON_CLIENT));
        createRootRegion(regionName, factory.create());
      }
    });
  }

  private void stopBridgeServer(VM server) {
    server.invoke(new CacheSerializableRunnable("Stop Server") {
      public void run2() throws CacheException {
        stopBridgeServers(getCache());
      }
    });
  }
}

