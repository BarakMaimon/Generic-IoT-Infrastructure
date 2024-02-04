import DB.IotCrudManager;
import com.sun.media.sound.InvalidDataException;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;


public class HttpGatewayServer {
    private final MultiProtocolServer multiProtocolServer;
    private final RequestHandler requestHandler;
    private final PlugAndPlay plugAndPlay;
    private final IotCrudManager iotCrudManager;

    public HttpGatewayServer() {
        this.multiProtocolServer = new MultiProtocolServer();
        this.requestHandler = new RequestHandler();
        try {
            this.plugAndPlay = new PlugAndPlay("Callable", "call",
                    "/home/barak/infinity/git/barak.maimon/projects/generic_IOT_infrastruction/src/DropJar");
        } catch (IOException e) {
            throw new RuntimeException("couldn't upload server.... ");
        }
        this.iotCrudManager = new IotCrudManager("mongodb://localhost:27017");

        this.requestHandler.factory.add("CompanyPOST", new NewCompany());
        this.requestHandler.factory.add("ProductPOST", new NewProduct());
        this.requestHandler.factory.add("IOTPOST", new NewIOT());
        this.requestHandler.factory.add("UpdatePOST", new NewUpdate());

    }

    public void start() {
        try {
            this.multiProtocolServer.addTCPConnection(8085);
            this.multiProtocolServer.start();
            new Thread(this.plugAndPlay).start();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("couldn't run server.... ");
        }
    }

    public void stop() {
        this.multiProtocolServer.stop();
    }

    private void handle(ByteBuffer buffer, Communicator communicator) {
        requestHandler.handle(buffer, communicator);
    }

    /*==============================================================================================*/
    /*================================= Communicator ========================================*/

    private interface Communicator {
        ByteBuffer receive();

        void send(ByteBuffer buffer);

    }

    /*==============================================================================================*/
    /*================================= API Commands =============================================*/

    private interface Command {
        void exec() throws InvalidDataException, InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException, SQLException; //TODO fix Exceptions
    }

    //TODO validator
    private class NewCompany implements Function<JsonObject, Command> {
        @Override
        public Command apply(JsonObject s) {
            return () -> {
                String[] fields = {"company_name", "company_address", "contact_name",
                        "contact_phone", "contact_email", "service_fee"};

                if (!isValid(s,fields)){throw new InvalidDataException("missing keys, see documentation");}
                iotCrudManager.registerCompanyCRUD(s);
            };
        }
    }

    private class NewProduct implements Function<JsonObject, Command> {
        @Override
        public Command apply(JsonObject s) {
            return () -> {
                String[] fields = {"company_name" , "product_name", "product_description"};
                if (!isValid(s,fields)){throw new InvalidDataException("missing keys, see documentation");}
                iotCrudManager.registerProductCRUD(s);
            };
        }
    }

    private class NewIOT implements Function<JsonObject, Command> {
        @Override
        public Command apply(JsonObject s) {
            return () -> {
                String[] fields = {"company_name" , "product_name", "serial_number"};
                if (!isValid(s,fields)){throw new InvalidDataException("missing keys, see documentation");}
                iotCrudManager.registerIotCRUD(s);
            };
        }
    }

    private class NewUpdate implements Function<JsonObject, Command> {
        @Override
        public Command apply(JsonObject s) {
            return () -> {
                String[] fields = {"company_name" , "product_name", "serial_number"};
                if (!isValid(s,fields)){throw new InvalidDataException("missing keys, see documentation");}
                iotCrudManager.updateCRUD(s);
            };
        }
    }


    /*============================================================================================*/
    /*================================= Request Handler ==========================================*/

    private static class RequestHandler {
        private final ThreadPool threadPool;
        private final Factory<String, JsonObject> factory;

        private RequestHandler() {
            this.threadPool = new ThreadPool(Runtime.getRuntime().availableProcessors());//TODO num of threads
            this.factory = new Factory<>();
        }

        private void handle(ByteBuffer buffer, Communicator communicator) {
            threadPool.submit(createRunnable(buffer, communicator), ThreadPool.Priority.DEFAULT);
        }

        private JsonObject getBody(ByteBuffer request) { //TODO fix ex exception
            return bytesToJson(request);
        }

        private static class Factory<K, D> {
            private final Map<K, Function<D, Command>> commands;

            public Factory() {
                commands = new HashMap<>();
            }

            private void add(K key, Function<D, Command> command) {
                commands.put(key, command);
            }

            private Command create(K key, D data) {
                return commands.get(key).apply(data);
            }
        }

        private Runnable createRunnable(ByteBuffer buffer, Communicator communicator) {
            return () -> {
                try {
                    JsonObject bodyRequest = getBody(buffer);
                    buffer.flip();
                    String request = convertHttpMethod(new String(buffer.array(),0,buffer.remaining()));
                    Command command = factory.create(request,bodyRequest);
                    command.exec();
                } catch (Exception e) { // TODO return error
                    communicator.send(ByteBuffer.wrap("400 Operation failed".getBytes()));
                    return;
                }
                communicator.send(ByteBuffer.wrap("200 Operation succeed".getBytes()));
            };
        }

    }

    /*============================================================================================*/
    /*================================ Multi Protocol Server =======================================*/

    private class MultiProtocolServer {
        private final CommunicationManager communicationManger;
        private final MessageManager messageManager;

        public MultiProtocolServer() {
            this.communicationManger = new CommunicationManager();
            messageManager = new MessageManager();
        }

        public void addTCPConnection(int clientPort) throws IOException { //TODO error handling
            this.communicationManger.addTCPConnection(clientPort);
        }

        public void stop() {
            this.communicationManger.stop();
        }

        public void start() throws IOException, ClassNotFoundException {//TODO error handling
            this.communicationManger.start();
        }

        /*=================================================================================================*/
        /*===================================== Massage Handlers =====================================*/
        /*=================================================================================================*/

        private class MessageManager {
            public void handle(Communicator communicator) throws IOException, ClassNotFoundException {//TODO error handling
                ByteBuffer byteBuffer = communicator.receive();
                if (byteBuffer == null) {
                    return;
                }
                System.out.println(new String(byteBuffer.array(),0,byteBuffer.remaining()));
                HttpGatewayServer.this.handle(byteBuffer, communicator);
            }
        }

        /*=================================================================================================*/
        /*===================================== Communication Manager =====================================*/
        /*=================================================================================================*/

        private class CommunicationManager {

            private final Selector selector;
            private final AtomicBoolean isRunning;
            private final SelectorRunner selectorRunner;


            public CommunicationManager() {
                try {
                    this.selector = Selector.open();
                } catch (IOException e) {//TODO error handling
                    throw new RuntimeException(e);
                }
                this.isRunning = new AtomicBoolean(true);
                this.selectorRunner = new SelectorRunner();
            }

            public void addTCPConnection(int TCPClientPort) throws IOException { //TODO error handling
                ServerSocketChannel tcpServerSocket = ServerSocketChannel.open();
                tcpServerSocket.configureBlocking(false);
                tcpServerSocket.bind(new InetSocketAddress("localhost", TCPClientPort));
                tcpServerSocket.register(selector, SelectionKey.OP_ACCEPT);
            }

            public void start() {
                new Thread(this.selectorRunner).start();
            }

            public void stop() {
                this.isRunning.set(false);
            }

            /*================================ Selector Runner ==============================================*/

            private class SelectorRunner implements Runnable {
                private final TCPRegister tcpRegister;

                public SelectorRunner() {

                    this.tcpRegister = new TCPRegister();

                }

                @Override
                public void run() {
                    Set<SelectionKey> selectedKeys = null;
                    while (isRunning.get()) {
                        try {
                            selector.select();

                            selectedKeys = selector.selectedKeys();
                            Iterator<SelectionKey> iter = selectedKeys.iterator();

                            while (iter.hasNext()) {
                                SelectionKey key = iter.next();

                                if (key.isAcceptable()) {
                                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                                    this.tcpRegister.TCPAccept(serverSocketChannel);

                                } else if (key.isReadable()) {
                                    TCPCommunicator tcpCommunicator = (TCPCommunicator) key.attachment();
                                        MultiProtocolServer.this.messageManager.handle(tcpCommunicator);
                                }
                                iter.remove();
                            }
                        } catch (IOException | ClassNotFoundException e) {//TODO error handling
                            throw new RuntimeException(e);
                        }

                    }
                    assert selectedKeys != null;
                    selectedKeys.clear();
                }

            }


            /*======================================== TCP register =================================================*/

            private class TCPRegister {
                public TCPCommunicator TCPAccept(ServerSocketChannel serverSocketChannel) {
                    try {
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        socketChannel.configureBlocking(false);
                        SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ);
                        TCPCommunicator tcpCommunicator = new TCPCommunicator(socketChannel);
                        key.attach(tcpCommunicator);
                        return tcpCommunicator;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            /*========================================== Communicators =================================================*/

            private class TCPCommunicator implements Communicator {
                private final SocketChannel clientSocketChannel;
                private static final String HTTP_RESPONSE_TEMPLATE = "HTTP/1.1 %s %s\r\n" +
                        "Content-Type: application/json\r\n" +
                        "\r\n" +
                        "{\r\n" +
                        "  \"message\": \"%s\"\r\n" +
                        "}";


                public TCPCommunicator(SocketChannel clientSocketChannel) {
                    this.clientSocketChannel = clientSocketChannel;
                }

                @Override
                public ByteBuffer receive() {
                    try {
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int bytesRead = clientSocketChannel.read(buffer);
                        buffer.flip();
                        if (bytesRead == -1) {
                            clientSocketChannel.close();
                            return null;
                        }

                        return buffer;
                    } catch (IOException e) {//TODO error handling
                        throw new RuntimeException(e);
                    }
                }


                @Override
                public void send(ByteBuffer buffer) {
                    try {
                        if (!clientSocketChannel.isOpen() || !clientSocketChannel.isConnected()) {
                            System.out.println("SocketChannel is not open or connected!");
                            return;
                        }

                        // Assuming the status code is the first byte in the buffer
                        String statusCode = new String(buffer.array(), 0, 3); // Assuming a 3-character status code
                        String responseMsg = new String(buffer.array(), 3, buffer.remaining() - 3);

                        String response = String.format(HTTP_RESPONSE_TEMPLATE, statusCode, getHttpStatusReason(statusCode), responseMsg);

                        System.out.println(response);
                        buffer = ByteBuffer.wrap(response.getBytes());

                        while (buffer.hasRemaining()) {
                            clientSocketChannel.write(buffer);
                        }

                        System.out.println("Sending message to " + clientSocketChannel);
                        clientSocketChannel.shutdownOutput();
                    } catch (IOException e) {//TODO error handling
                        throw new RuntimeException(e);
                    }
                }


                // Helper method to get the HTTP status reason phrase
                private String getHttpStatusReason(String statusCode) {
                    switch (statusCode) {
                        case "200":
                            return "OK";
                        case "400":
                            return "Bad Request";
                        // Add more status codes as needed
                        default:
                            return "Unknown";
                    }
                }



            }

        }
    }

    /*==================================================================================================*/
    /*===================================== Plug and Play ===============================================*/

    private class PlugAndPlay implements Runnable {
        MonitorDir monitorDir;
        DynamicJarLoader dynamicJarLoader;
        String methodName;

        public PlugAndPlay(String interfaceName, String methodName, String pathToDir) throws IOException {//TODO error handling
            this.monitorDir = new MonitorDir(pathToDir);
            this.dynamicJarLoader = new DynamicJarLoader(interfaceName);
            this.methodName = methodName;
        }

        @Override
        public void run() {
            while (HttpGatewayServer.this.multiProtocolServer.communicationManger.isRunning.get()) {
                try {
                    File file = this.monitorDir.watchDirectoryPath();
                    if (file == null) {
                        throw new RuntimeException("no file"); // TODO error handling
                    }
                    ArrayList<Class<?>> classes = this.dynamicJarLoader.load(file.getAbsolutePath());
                    for (Class<?> cls : classes) {
                        addToFactory(cls);
                    }

                } catch (Exception e) {// TODO error handling
                    System.out.println(e.getMessage());
                }
            }

        }

        private void addToFactory(Class<?> cls) {//TODO error handling
            Function<JsonObject, Command> function = s -> () -> {
                Method method = cls.getMethod(this.methodName);
                System.out.println((String) method.invoke(cls.newInstance()));
            };

            HttpGatewayServer.this.requestHandler.factory.add(cls.getName(), function);
            System.out.println("Created new API method : " + cls.getName());
        }

        private class MonitorDir {
            private final String pathName;

            public MonitorDir(String pathName) {
                this.pathName = pathName;
            }


            public File watchDirectoryPath() {
                Path path = Paths.get(this.pathName);

                FileSystem fs = path.getFileSystem();

                try {
                    WatchService watchService = fs.newWatchService();
                    path.register(watchService, ENTRY_CREATE);

                    WatchKey key;
                    do {
                        key = watchService.take();

                        for (WatchEvent<?> watchEvent : key.pollEvents()) {
                            Path newFile = (Path) watchEvent.context();
                            Path absoultPath = path.resolve(newFile);
                            File file = absoultPath.toFile();
                            while (!file.canRead()) ;
                            return file;
                        }
                    } while (key.reset());

                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e); // TODO error handaling
                }
                return null;
            }

        }

        private class DynamicJarLoader {
            private final String interfaceName;


            public DynamicJarLoader(String interfaceName) {
                this.interfaceName = interfaceName;
            }


            public ArrayList<Class<?>> load(String pathToJar) { // try catch?
                JarFile jarFile = null;
                Enumeration<JarEntry> enumeration;
                ArrayList<Class<?>> availableClasses;
                URL[] urls;
                try {
                    jarFile = new JarFile(pathToJar);
                    enumeration = jarFile.entries();
                    availableClasses = new ArrayList<>();
                    urls = new URL[]{new URL("jar:file:" + jarFile.getName() + "!/")};
                } catch (IOException e) {
                    throw new RuntimeException(e); //TODO error handling
                }

                URLClassLoader cl = URLClassLoader.newInstance(urls);

                while (enumeration.hasMoreElements()) {
                    JarEntry je = enumeration.nextElement();
                    String className = je.getName();
                    if ((className.endsWith(".class"))) {
                        try {
                            className = className.substring(0, className.length() - 6);
                            className = className.replace('/', '.');
                            Class<?> myClass = cl.loadClass(className);
                            Class<?>[] interfaces = myClass.getInterfaces();

                            for (Class<?> cls : interfaces) {
                                if (this.interfaceName.equals(cls.getSimpleName())) {
                                    availableClasses.add(myClass);
                                    break;
                                }
                            }

                        } catch (ClassNotFoundException e) {
                            System.out.println("Class " + className + " was not found!" + e); //TODO error handling
                        }
                    }
                }
                return availableClasses;
            }
        }

    }
//================================================================================================================
//================================================================================================================
//================================================================================================================

    private static boolean isValid(JsonObject data, String[] keys){
        for (String key : keys){
            if(!data.containsKey(key)) {
                return false;
            }
        }

        return true;
    }

    public static String splitByteBuffer(ByteBuffer buffer) {
        // Convert ByteBuffer to String
        String content = StandardCharsets.UTF_8.decode(buffer).toString();

        // Use regex to split based on two consecutive opening curly braces
        String[] parts = content.split("\\{");

        if(parts.length < 2){
            return null;
        }

        return "{" + parts[1];
    }

    private static JsonObject bytesToJson(ByteBuffer buffer) {
        if (null == buffer) {
            return null;
        }
        String body = splitByteBuffer(buffer);
        if(null == body){
            return null;
        }

        try (StringReader reader = new StringReader(body)) {
            return Json.createReader(reader).readObject();
        }
    }

    public static String convertHttpMethod(String httpRequest) {
        // Use regex to extract method and path from the first line
        Pattern pattern = Pattern.compile("^(?<method>[A-Z]+)\\s+(?<path>/\\S+)\\s+HTTP/\\d\\.\\d\\s*\\r?\\n", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(httpRequest);

        if (matcher.find()) {
            String method = matcher.group("method");
            String path = matcher.group("path");

            // Combine method and path in a generic format
            return path.substring(1) + method; // Remove leading slash from the path
        }

        throw new IllegalArgumentException("Invalid HTTP request format");
    }

    public static void main(String[] args) {
        HttpGatewayServer gatewayServer = new HttpGatewayServer();
        gatewayServer.start();
    }
}

