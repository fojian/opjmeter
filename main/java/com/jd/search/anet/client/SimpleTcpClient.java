package com.jd.search.anet.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;

public class SimpleTcpClient {
	private String host = "127.0.0.1";
    private int port = 0;
    private int timeoutMs = 100;
    private SocketChannel socketChannel = null;

    public SimpleTcpClient(String host, int port, int timeoutMs) throws IOException {
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;

        this.socketChannel = SocketChannel.open();
        this.socketChannel.configureBlocking(true);
        this.socketChannel.socket().setSoTimeout(this.timeoutMs);

        this.socketChannel.socket().connect(new InetSocketAddress(this.host, this.port), this.timeoutMs);
    }


    public long write(byte[] data) throws IOException {
        return this.socketChannel.write(ByteBuffer.wrap(data));
    }

    public long write(byte[] head, byte[] body) throws IOException  {
        ByteBuffer[] bufferArray = {ByteBuffer.wrap(head), ByteBuffer.wrap(body)};
        return this.socketChannel.write(bufferArray);
    }

    public byte[] read(int dataLen) throws IOException {
        ByteBuffer dataBuf = ByteBuffer.allocate(dataLen);
        InputStream inStream = socketChannel.socket().getInputStream();
        ReadableByteChannel readChannel = Channels.newChannel(inStream);
        while (dataBuf.hasRemaining()) {
            readChannel.read(dataBuf);
        }
        return dataBuf.array();
    }

    public void close() {
        if(this.socketChannel != null) {
            try {
                this.socketChannel.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                this.socketChannel = null;
            }
        }
    }
}
