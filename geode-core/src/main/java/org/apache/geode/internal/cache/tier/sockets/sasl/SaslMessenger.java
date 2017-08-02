package org.apache.geode.internal.cache.tier.sockets.sasl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class SaslMessenger {
  private DataInput inputStream;
  private DataOutput outputStream;

  public SaslMessenger(DataInput inputStream, DataOutput outputStream) {
    this.inputStream = inputStream;
    this.outputStream = outputStream;
  }

  public void sendMessage(byte[] capture) throws IOException {
    outputStream.writeInt(capture.length);
    outputStream.write(capture);
  }

  public byte[] readMessage() throws IOException {
    int byteArrayLength = inputStream.readInt();
    byte[] ret = new byte[byteArrayLength];
    inputStream.readFully(ret);
    return ret;
  }
}
