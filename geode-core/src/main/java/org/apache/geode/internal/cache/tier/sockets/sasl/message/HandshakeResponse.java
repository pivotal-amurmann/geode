package org.apache.geode.internal.cache.tier.sockets.sasl.message;


import java.nio.charset.Charset;

import javax.security.sasl.AuthenticationException;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class HandshakeResponse {
  private String correlationID;
  private String mechanism;

  public HandshakeResponse(String correlationID, String mechanism){
    this.correlationID = correlationID;
    this.mechanism = mechanism;
  }

  public byte[] toByteArray() {
    byte[] correlationBytes = correlationID.getBytes(Charset.forName("UTF8"));
    byte[] mechanismBytes = mechanism.getBytes(Charset.forName("UTF8"));

    ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
    dataOutput.write(correlationBytes);
    dataOutput.writeByte(0);
    dataOutput.write(mechanismBytes);
    dataOutput.writeByte(0);

    return dataOutput.toByteArray();
  }

  public static HandshakeResponse from(byte[] message) throws AuthenticationException {
    String[] strings = new String(message, Charset.forName("UTF8")).split("\00");
    if(strings.length < 2)
      throw new AuthenticationException("Malformed handshake");

    return new HandshakeResponse(strings[0], strings[1]);
  }

  public String getMechanism(){
    return mechanism;
  }

  public String getCorrelationId(){
    return correlationID;
  }
}
