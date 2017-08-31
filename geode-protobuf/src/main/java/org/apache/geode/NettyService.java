package org.apache.geode;

import java.io.IOException;

import org.apache.geode.cache.Cache;
import org.apache.geode.distributed.internal.DistributionConfig;
import org.apache.geode.internal.admin.SSLConfig;
import org.apache.geode.internal.cache.CacheService;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.net.SSLConfigurationFactory;
import org.apache.geode.internal.security.SecurableCommunicationChannel;
import org.apache.geode.management.internal.beans.CacheServiceMBeanBase;
import org.apache.geode.security.SecurableCommunicationChannels;

public class NettyService implements CacheService {
  private NettyServer nettyServer;

  @Override
  public void init(Cache cache) {
    InternalCache internalCache = (InternalCache) cache;
    DistributionConfig
        distributionConfig =
        internalCache.getInternalDistributedSystem().getConfig();
    SSLConfig
        sslConfig =
        SSLConfigurationFactory.getSSLConfigForComponent(distributionConfig,
            SecurableCommunicationChannel.SERVER);
    int port = Integer.getInteger("nettyPort", 40405);

    nettyServer = new NettyServer(port, cache, sslConfig);
    try {
      nettyServer.run();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Class<? extends CacheService> getInterface() {
    return this.getClass(); // HACK?
  }

  @Override
  public CacheServiceMBeanBase getMBean() {
    return null; // maybe?
  }

  @Override
  public void close() {
    nettyServer.close();
  }
}
