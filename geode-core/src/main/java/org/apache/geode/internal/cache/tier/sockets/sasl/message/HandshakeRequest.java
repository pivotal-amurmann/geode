package org.apache.geode.internal.cache.tier.sockets.sasl.message;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.Charset;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class HandshakeRequest {
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


  private void writeBytesToStream(DataOutput out) throws IOException {
    byte[] versionBytes = version.getBytes(Charset.forName("UTF8"));
    byte[] mechanismBytes = mechanism.getBytes(Charset.forName("UTF8"));
    byte[] correlationBytes = correlationID.getBytes(Charset.forName("UTF8"));
    byte[] clientIDBytes = clientID.getBytes(Charset.forName("UTF8"));
    out.writeInt(versionBytes.length);
    out.write(versionBytes);
    out.writeInt(correlationBytes.length);
    out.write(correlationBytes);
    out.writeInt(clientIDBytes.length);
    out.write(clientIDBytes);
    out.writeInt(mechanismBytes.length);
    out.write(mechanismBytes);
  }

  private void readExternal(DataInput in) throws IOException, ClassNotFoundException {
  }
}
