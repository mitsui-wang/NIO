package com.mitsui.nio;

import com.google.common.base.Throwables;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;

/**
 * Created by admin on 2018/11/12.
 */
public class MainServer implements Runnable {
    Selector selector;
    static final Integer bufSize = 5;

    public void init() {
        try {
            selector = Selector.open();
            //启动一个监听的通道
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            serverChannel.setOption(StandardSocketOptions.SO_RCVBUF, 1024);
            serverChannel.bind(new InetSocketAddress("localhost", 8888), 100);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            System.out.println(Throwables.getStackTraceAsString(e));
            throw new RuntimeException();
        }

    }

    public void run() {
        while (true) {
            try {
                selector.select(100000L);
                Set<SelectionKey> keys = selector.selectedKeys();
                try {
                    System.out.println("ok");
                    for (SelectionKey key : keys) {
                        if (key.isValid() && key.isAcceptable()) {
                            //这里是连接刚刚连上的时候在这里既然连接上了
                            // 我们需要把这个具体的连接拿出来
                            SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
                            clientChannel.configureBlocking(false);
                            clientChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
                            //拿出来之后我们需要改变这个NIO的状态然后将这个通道注册回去
                            clientChannel.register(key.selector(), OP_READ, ByteBuffer.allocate(bufSize));
                        } else if (key.isValid() && key.isReadable()) {
                            //获取到响应的通道
                            ByteBuffer bufRecv = ByteBuffer.allocate(10000);
                            SocketChannel clientChannel = (SocketChannel) key.channel();
                            //获取连接中的buf
                            ByteBuffer buf = (ByteBuffer) key.attachment();
                            int bytesRead = 0;
                            boolean isEnd = false;
                            while (!isEnd) {
                                try {
                                    bytesRead = clientChannel.read(buf);
                                } catch (IOException e) {
                                   // key.cancel();
//                                    clientChannel.socket().close();
                                    clientChannel.close();
                                    System.out.println("error");
                                    break;
                                }
                                if (bytesRead < 0) {
                                    key.cancel();
                                    clientChannel.socket().close();
                                    clientChannel.close();
                                    break;
                                } else if (bytesRead == bufSize) {
                                    buf.flip();
                                    bufRecv.put(buf);
                                } else if (bytesRead > 0 && bytesRead < bufSize) {
                                    buf.flip();
                                    bufRecv.put(buf);
                                    isEnd = true;
                                } else {
                                    isEnd = true;
                                }

                                buf.clear();
                            }

                            bufRecv.flip();
//                            key.interestOps(SelectionKey.OP_WRITE);
                            System.out.println("service: Get data: " + new String(bufRecv.array()));

                        } else if (key.isValid() && key.isWritable()) {
                            ByteBuffer buf = (ByteBuffer) key.attachment();
                            buf.clear();
                            buf.put("11".getBytes());
                            buf.flip();
                            SocketChannel clientChannel = (SocketChannel) key.channel();
                            clientChannel.finishConnect();
                            try {
                                clientChannel.write(buf);
                            } catch (IOException e) {
                                key.cancel();
                                clientChannel.socket().close();
                                clientChannel.close();
                            }
                            if (!buf.hasRemaining()) {
                                key.interestOps(OP_READ);
                            }
                            buf.compact();
                        } else {
                            key.cancel();
                        }
                    }
                } finally {
                    keys.clear();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("some thing wrong with this");
            }
        }
    }
}
