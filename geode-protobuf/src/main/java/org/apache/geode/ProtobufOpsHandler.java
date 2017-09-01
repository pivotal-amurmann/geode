package org.apache.geode;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.apache.geode.internal.cache.tier.sockets.MessageExecutionContext;
import org.apache.geode.internal.protocol.protobuf.ClientProtocol;
import org.apache.geode.protocol.protobuf.ProtobufOpsProcessor;
import org.apache.geode.protocol.protobuf.utilities.ProtobufUtilities;

public class ProtobufOpsHandler extends ChannelInboundHandlerAdapter {
  private MessageExecutionContext executionContext;
  private final ProtobufOpsProcessor protobufOpsProcessor;

  public ProtobufOpsHandler(MessageExecutionContext executionContext, ProtobufOpsProcessor protobufOpsProcessor) {
    this.executionContext = executionContext;
    this.protobufOpsProcessor = protobufOpsProcessor;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    ClientProtocol.Message message = (ClientProtocol.Message) msg;

//    System.out.println(
//        "ops handler Thread.currentThread().getName() = " + Thread
//            .currentThread().getName() + "+ Thread.currentThread().getID() = " + Thread.currentThread().getId());

    ClientProtocol.Response
        response =
        protobufOpsProcessor.process(message.getRequest(), executionContext);

    ClientProtocol.MessageHeader responseHeader =
        ProtobufUtilities.createMessageHeaderForRequest(message);
    ClientProtocol.Message responseMessage =
        ProtobufUtilities.createProtobufResponse(responseHeader, response);

    ctx.write(responseMessage);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }
}

