import java.io.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TestTCP implements Runnable {

    public void run() {
        try {
            // Create a SocketChannel object.
            SocketChannel socketChannel = SocketChannel.open();


            // Connect the SocketChannel to the server.
            InetSocketAddress serverAddress = new InetSocketAddress("localhost", 8085);
            socketChannel.connect(serverAddress);
            System.out.println(socketChannel);


                // Create a ByteBuffer object to store the data you want to send.
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);

                // Write the data to the ByteBuffer object.
                byteBuffer.put(GatewayServer.serialize("RegisterCompany//company_name:Radian//company_address:Beer Sheva//" +
                        "contact_name:Bar//contact_phone:0508328747//contact_email:bar@gmail.com//service_fee:600"));
                byteBuffer.flip();
                // Write the ByteBuffer object to the SocketChannel.

                socketChannel.write(byteBuffer);

                ByteBuffer massage = ByteBuffer.allocate(1500);

                System.out.println(socketChannel.read(massage));
                massage.flip();
                String message = GatewayServer.deserialize(massage);

                System.out.println(message);




            socketChannel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void main(String[] args) throws IOException {
//        for (int i = 0; i < 5; ++i) {
        new Thread(new TestTCP()).start();
//        }
    }
}

class myMessage implements Message<String, String> {
    private final String methods;
    private final String data;

    public myMessage(String method, String data) {
        this.data = data;
        this.methods = method;
    }

    @Override
    public String getKey() {
        return this.methods;
    }

    @Override
    public String getMessage() {
        return this.data;
    }
}
