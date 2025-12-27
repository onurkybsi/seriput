package io.seriput.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

/**
 * Represents the identifier for each Seriput clients.
 *
 * @param address client address
 * @param port client address port
 */
record SeriputClient(InetAddress address, int port) {
  static SeriputClient from(SocketChannel channel) throws IOException {
    SocketAddress address = channel.getRemoteAddress();
    if (!(address instanceof InetSocketAddress inet)) {
      throw new IllegalStateException("Unexpected SocketAddress: " + address.getClass().getName());
    }
    return new SeriputClient(inet.getAddress(), inet.getPort());
  }
}
