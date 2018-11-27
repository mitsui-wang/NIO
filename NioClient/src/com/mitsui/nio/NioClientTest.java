package com.mitsui.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * Created by XH on 2017/9/7.
 */
public class NioClientTest {

    /*标识数字*/
    private static int flag = 0;

    public static void main(String[] args) throws Exception {
        // 打开socket通道
        SocketChannel socketChannel = SocketChannel.open();
        // 设置为非阻塞方式
        socketChannel.configureBlocking(false);
        // 打开选择器
        Selector selector = Selector.open();
        // 注册连接服务端socket动作
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        // 连接
        socketChannel.connect(new InetSocketAddress("localhost", 8888));
        // 分配缓冲区大小内存

        Set<SelectionKey> selectionKeys;
        SocketChannel client;
        String receiveText;
        String sendText;
        int count=0;

        while (true) {
            selector.select();
            //返回此选择器的已选择键集。
            selectionKeys = selector.selectedKeys();
            for (SelectionKey selectionKey:selectionKeys) {
                if (selectionKey.isConnectable()) {
                    client = (SocketChannel) selectionKey.channel();
                    // 判断此通道上是否正在进行连接操作。
                    // 完成套接字通道的连接过程。
                    if (client.isConnectionPending()) {
                        client.finishConnect();
                        System.out.println("connect success");
                        ByteBuffer sendbuffer = ByteBuffer.allocate(1024);
                        sendbuffer.clear();
                        sendbuffer.put("iamclient".getBytes());
                        sendbuffer.flip();
                        while (true) {
                            Thread.sleep(3000L);
                            try {
                                client.write(sendbuffer);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                    //发送完之后就复位，准备读取输入
                    client.register(selector, SelectionKey.OP_READ);
                } else if (selectionKey.isReadable()) {
                    client = (SocketChannel) selectionKey.channel();
                    ByteBuffer receivebuffer = ByteBuffer.allocate(1024);
                    count=client.read(receivebuffer);
                    if(count>0){
                        receiveText = new String( receivebuffer.array(),0,count);
                        System.out.println("client: recive data:"+receiveText);
                        client.register(selector, SelectionKey.OP_WRITE);
                    }

                } else if (selectionKey.isWritable()) {
                    Thread.sleep(1000);
                    ByteBuffer sendbuffer = ByteBuffer.allocate(1024);
                    sendbuffer.clear();
                    client = (SocketChannel) selectionKey.channel();
                    sendbuffer.put("hello world".getBytes());
                    sendbuffer.flip();
                    client.finishConnect();
                    client.write(sendbuffer);
                    client.register(selector, SelectionKey.OP_READ);
                }
            }
            selectionKeys.clear();
        }
    }
}
