package org.apache.geode;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.apache.geode.internal.protocol.protobuf.ClientProtocol;
import org.apache.geode.internal.protocol.protobuf.RegionAPI;

public class EchoNettyChannelHandler extends ChannelInboundHandlerAdapter {
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    ClientProtocol.Message message = (ClientProtocol.Message) msg;
    String regionName = message.getRequest().getPutRequest().getRegionName();
    System.out.println("===> regionName: " + regionName);
    ClientProtocol.Response.Builder response = ClientProtocol.Response.newBuilder().setPutResponse(
        RegionAPI.PutResponse.newBuilder()
            .getDefaultInstanceForType());
    ctx.write(response);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }
}
