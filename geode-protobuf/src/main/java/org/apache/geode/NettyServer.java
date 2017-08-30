package org.apache.geode;

import java.io.IOException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
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
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import org.apache.geode.cache.Cache;
import org.apache.geode.internal.cache.tier.sockets.MessageExecutionContext;
import org.apache.geode.internal.protocol.protobuf.ClientProtocol;
import org.apache.geode.protocol.protobuf.ProtobufOpsProcessor;
import org.apache.geode.protocol.protobuf.ProtobufSerializationService;
import org.apache.geode.protocol.protobuf.registry.OperationContextRegistry;
import org.apache.geode.security.server.NoOpAuthorizer;

public class NettyServer {
  /**
   * The number of threads that will work on handling requests
   */
  private final int numWorkerThreads = 16;

  /**
   * The number of threads that will work socket selectors
   */
  private final int numSelectorThreads = 16;

  /**
   * The cache instance pointer on this vm
   */
  private final Cache cache;

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
  private final boolean singleThreadPerConnection = false;

  public NettyServer(int port, Cache cache) {
    this.port = port;
    this.cache = cache;
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
            MessageExecutionContext messageExecutionContext = new MessageExecutionContext(cache, new NoOpAuthorizer());
            ProtobufOpsProcessor
                protobufOpsProcessor =
                new ProtobufOpsProcessor(new ProtobufSerializationService(),
                    new OperationContextRegistry());

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
//            pipeline.addLast("echo", new EchoNettyChannelHandler());

            pipeline.addLast("protobufOpsHandler", new ProtobufOpsHandler(messageExecutionContext, protobufOpsProcessor));
          }
        }).option(ChannelOption.SO_REUSEADDR, true).option(ChannelOption.SO_RCVBUF, getBufferSize())
        .childOption(ChannelOption.SO_KEEPALIVE, true)
        .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

    // Bind and start to accept incoming connections.
    ChannelFuture f = b.bind(port).sync();
    this.serverChannel = f.channel();
  }

  private int getBufferSize() {
    return 65000;
  }

//  public static void main(String[] args) throws Exception {
//    int port;
//    if (args.length > 0) {
//      port = Integer.parseInt(args[0]);
//    } else {
//      port = 8080;
//    }
//    new NettyServer(port).run();
//  }
}