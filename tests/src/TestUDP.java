import java.io.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class TestUDP implements Runnable {

    public void run() {
        try {
            // Create a SocketChannel object.
            //SocketChannel socketChannel = SocketChannel.open();
            DatagramChannel datagramChannel = DatagramChannel.open();

            // Connect the SocketChannel to the server.
            //socketChannel.connect(serverAddress);
            InetSocketAddress serverAddress = new InetSocketAddress("localhost", 8081);
            datagramChannel.connect(serverAddress);

            for (int i = 0; i < 3; ++i) {

                // Create a ByteBuffer object to store the data you want to send.
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);

                // Write the data to the ByteBuffer object.
                byteBuffer.put(GatewayServer.serialize("RegisterIOT@Tadiran"));
                byteBuffer.flip();
                // Write the ByteBuffer object to the SocketChannel.
                //socketChannel.write(byteBuffer);
                datagramChannel.write(byteBuffer);

                ByteBuffer massage = ByteBuffer.allocate(500);

                System.out.println(datagramChannel.read(massage));
                massage.flip();
                String message = GatewayServer.deserialize(massage);

                System.out.println(message);


            }
            datagramChannel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) throws IOException {
        /*for (int i = 0; i < 5; ++i) {*/
        new Thread(new TestUDP()).start();
//        }
    }
}
