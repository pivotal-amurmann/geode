package org.apache.geode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.bootstrap.ServerBootstrap;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.oio.OioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import org.apache.geode.cache.Cache;
import org.apache.geode.distributed.internal.InternalDistributedSystem;
import org.apache.geode.internal.protocol.protobuf.ClientProtocol;
import org.apache.geode.redis.GeodeRedisServer;
import org.apache.geode.redis.internal.ByteToCommandDecoder;
import org.apache.geode.redis.internal.Coder;
import org.apache.geode.redis.internal.ExecutionHandlerContext;

/**
 * Discards any incoming data.
 */
public class NettyServer {
  /**
   * Thread used to start main method
   */
  private static Thread mainThread = null;

  /**
   * The default Redis port as specified by their protocol, {@code DEFAULT_REDIS_SERVER_PORT}
   */
  public static final int DEFAULT_REDIS_SERVER_PORT = 6379;

  /**
   * The number of threads that will work on handling requests
   */
  private final int numWorkerThreads = 16;

  /**
   * The number of threads that will work socket selectors
   */
  private final int numSelectorThreads = 16;

  /**
   * The actual port being used by the server
   */
  private final int serverPort = 40405;

  /**
   * Connection timeout in milliseconds
   */
  private static final int connectTimeoutMillis = 1000;

  /**
   * Temporary constant whether to use old single thread per connection model for worker group
   */
  private boolean singleThreadPerConnection;

  /**
   * The cache instance pointer on this vm
   */
  private Cache cache;

  /**
   * Channel to be closed when shutting down
   */
  private Channel serverChannel;

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;
  private final static int numExpirationThreads = 1;

  private boolean shutdown;
  private boolean started;

  private int port;

  public NettyServer(int port) {
    this.port = port;
  }

  public void run() throws Exception {
    try {
      startServer();
      // Wait until the server socket is closed.
      // In this example, this does not happen, but you can do that to gracefully
      // shut down your server.
//      serverChannel.closeFuture().sync();
    } finally {
//      workerGroup.shutdownGracefully();
//      bossGroup.shutdownGracefully();
    }
  }

  private void startServer() throws IOException, InterruptedException {
    ThreadFactory selectorThreadFactory = new ThreadFactory() {
      private final AtomicInteger counter = new AtomicInteger();

      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName("GeodeRedisServer-SelectorThread-" + counter.incrementAndGet());
        t.setDaemon(true);
        return t;
      }

    };

    ThreadFactory workerThreadFactory = new ThreadFactory() {
      private final AtomicInteger counter = new AtomicInteger();

      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setName("GeodeRedisServer-WorkerThread-" + counter.incrementAndGet());
        return t;
      }

    };

    bossGroup = null;
    workerGroup = null;
    Class<? extends ServerChannel> socketClass = null;
    if (singleThreadPerConnection) {
      bossGroup = new OioEventLoopGroup(Integer.MAX_VALUE, selectorThreadFactory);
      workerGroup = new OioEventLoopGroup(Integer.MAX_VALUE, workerThreadFactory);
      socketClass = OioServerSocketChannel.class;
    } else {
      bossGroup = new NioEventLoopGroup(this.numSelectorThreads, selectorThreadFactory);
      workerGroup = new NioEventLoopGroup(this.numWorkerThreads, workerThreadFactory);
      socketClass = NioServerSocketChannel.class;
    }
//    InternalDistributedSystem system = (InternalDistributedSystem) cache.getDistributedSystem();
//    String pwd = system.getConfig().getRedisPassword();
//    final byte[] pwdB = Coder.stringToBytes(pwd);
    ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup, workerGroup).channel(socketClass)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();

            // Decoder
            pipeline.addLast("frameDecoder",
                new ProtobufVarint32FrameDecoder());
            pipeline.addLast("protobufDecoder",
                new ProtobufDecoder(ClientProtocol.Message.getDefaultInstance()));

           // pipeline.addLast("echo2", new EchoNettyChannelHandler());


            // Encoder
            pipeline.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
            pipeline.addLast("protobufEncoder", new ProtobufEncoder());
            pipeline.addLast("echo", new EchoNettyChannelHandler());


          }
        }).option(ChannelOption.SO_REUSEADDR, true).option(ChannelOption.SO_RCVBUF, getBufferSize())
        .childOption(ChannelOption.SO_KEEPALIVE, true)
        .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

    // Bind and start to accept incoming connections.
    ChannelFuture f = b.bind(serverPort).sync();
    this.serverChannel = f.channel();
  }

  private int getBufferSize() {
    return 65000;
  }

  public static void main(String[] args) throws Exception {
    int port;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    } else {
      port = 8080;
    }
    new NettyServer(port).run();
  }
}