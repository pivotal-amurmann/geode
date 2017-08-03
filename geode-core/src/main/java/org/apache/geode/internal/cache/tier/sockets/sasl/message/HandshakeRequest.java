package org.apache.geode.internal.cache.tier.sockets.sasl.message;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.security.sasl.AuthenticationException;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class HandshakeRequest {
  public static final String VERSION="0.1";
  private final String version;
  private final String mechanism;
  private final String clientID;
  private final String correlationID;

  public HandshakeRequest(String version, String correlationID, String clientID, String mechanism) {
    this.version = version;
    this.mechanism = mechanism;
    this.clientID = clientID;
    this.correlationID = correlationID;
  }

  public byte[] toByteArray() throws IOException {
    ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
    this.writeBytesToStream(dataOutput);
    return dataOutput.toByteArray();
  }

  public String getMechanism() {
    return mechanism;
  }

  public String getClientID() {
    return clientID;
  }

  public String getCorrelationID() {
    return correlationID;
  }

  public String getVersion() {
    return version;
  }

  public static HandshakeRequest from(byte[] message) throws AuthenticationException {
    String[] strings = new String(message, Charset.forName("UTF8")).split("\00");
    if (strings.length > 0 && strings[0].equals(VERSION)) {
      if (strings.length != 4) {
        throw new AuthenticationException("Malformed handshake request.");
      }
      return new HandshakeRequest(strings[0], strings[1], strings[2], strings[3]);
    } else {
      throw new AuthenticationException("Unknown version number. Expected '" + VERSION + "'.");
    }
  }

  private void writeBytesToStream(DataOutput out) throws IOException {
    byte[] versionBytes = version.getBytes(Charset.forName("UTF8"));
    byte[] mechanismBytes = mechanism.getBytes(Charset.forName("UTF8"));
    byte[] correlationBytes = correlationID.getBytes(Charset.forName("UTF8"));
    byte[] clientIDBytes = clientID.getBytes(Charset.forName("UTF8"));
    out.write(versionBytes);
    out.writeByte(0);
    out.write(correlationBytes);
    out.writeByte(0);
    out.write(clientIDBytes);
    out.writeByte(0);
    out.write(mechanismBytes);
    out.writeByte(0);
  }
}
