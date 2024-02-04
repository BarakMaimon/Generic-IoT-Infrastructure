import DB.AdminDBManager;
import DB.IotCrudManager;
import com.sun.media.sound.InvalidDataException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;


public class GatewayServer {
    private final MultiProtocolServer multiProtocolServer;
    private final RequestHandler requestHandler;
    private final PlugAndPlay plugAndPlay;
    private final IotCrudManager iotCrudManager;
    private final AdminDBManager adminDBManager;

    public GatewayServer() {
        this.multiProtocolServer = new MultiProtocolServer();
        this.requestHandler = new RequestHandler();
        try {
            this.plugAndPlay = new PlugAndPlay("Callable", "call",
                    "/home/barak/infinity/git/barak.maimon/projects/generic_IOT_infrastruction/src/DropJar");
        } catch (IOException e) {
            throw new RuntimeException("couldn't upload server.... ");
        }
        try {
            this.adminDBManager = new AdminDBManager("jdbc:mysql://localhost:3306","root","Bm27102000");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        this.iotCrudManager = new IotCrudManager("mongodb://localhost:27017");

        this.requestHandler.factory.add("RegisterCompany", new NewCompany());
        this.requestHandler.factory.add("RegisterProduct", new NewProduct());
        this.requestHandler.factory.add("RegisterIOT", new NewIOT());
        this.requestHandler.factory.add("Update", new NewUpdate());

    }

    public void start() {
        try {
            this.multiProtocolServer.addTCPConnection(8085);
            this.multiProtocolServer.addUDPConnection(8081);
            this.multiProtocolServer.start();
            new Thread(this.plugAndPlay).start();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("couldn't run server.... ");
        }
    }

    public void stop() {
        this.multiProtocolServer.stop();
    }

    public static String deserialize(ByteBuffer buffer) {
        if (null == buffer) {
            return null;
        }
        return StandardCharsets.UTF_8.decode(buffer).toString();

    }

    public static ByteBuffer serialize(String message) {
        return ByteBuffer.wrap(message.getBytes());
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
    private class NewCompany implements Function<String, Command> {
        @Override
        public Command apply(String s) {
            return () -> {
                JsonObject data = parseStringToJson(s);
                String[] fields = {"company_name", "company_address", "contact_name",
                        "contact_phone", "contact_email", "service_fee"};

                if (!isValid(data,fields)){throw new InvalidDataException("missing keys, see documentation");}
                adminDBManager.registerComapny(data);
                iotCrudManager.registerCompanyCRUD(data);
            };
        }
    }

    private class NewProduct implements Function<String, Command> {
        @Override
        public Command apply(String s) {
            return () -> {
                JsonObject data = parseStringToJson(s);
                String[] fields = {"company_name" , "product_name", "product_description"};
                if (!isValid(data,fields)){throw new InvalidDataException("missing keys, see documentation");}
                adminDBManager.registerProduct(data);
                iotCrudManager.registerProductCRUD(data);
            };
        }
    }

    private class NewIOT implements Function<String, Command> {
        @Override
        public Command apply(String s) {
            return () -> {
                JsonObject data = parseStringToJson(s);
                String[] fields = {"company_name" , "product_name", "serial_number"};
                if (!isValid(data,fields)){throw new InvalidDataException("missing keys, see documentation");}
                iotCrudManager.registerIotCRUD(data);
            };
        }
    }

    private class NewUpdate implements Function<String, Command> {
        @Override
        public Command apply(String s) {
            return () -> {
                JsonObject data = parseStringToJson(s);
                String[] fields = {"company_name" , "product_name", "serial_number"};
                if (!isValid(data,fields)){throw new InvalidDataException("missing keys, see documentation");}
                iotCrudManager.updateCRUD(data);
            };
        }
    }


    /*============================================================================================*/
    /*================================= Request Handler ==========================================*/

    private static class RequestHandler {
        private final ThreadPool threadPool;
        private final Factory<String, String> factory;

        private RequestHandler() {
            this.threadPool = new ThreadPool(Runtime.getRuntime().availableProcessors());//TODO num of threads
            this.factory = new Factory<>();
        }

        private void handle(ByteBuffer buffer, Communicator communicator) {
            threadPool.submit(createRunnable(buffer, communicator), ThreadPool.Priority.DEFAULT);
        }

        private Entry<String, String> parse(String request) throws DataFormatException { //TODO fix ex exception
            Pattern PATTERN = Pattern.compile("^(.+?)//(.+)$");
            Matcher matcher = PATTERN.matcher(request);
            if (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                return new AbstractMap.SimpleEntry<>(key, value);
            }
           throw new DataFormatException();
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
                    String content = new String(buffer.array(),0,buffer.remaining());
                    Entry<String, String> entry = parse(content);
                    Command command = factory.create(entry.getKey(), entry.getValue());
                    command.exec();
                } catch (Exception e) { // TODO return error
                    communicator.send(ByteBuffer.wrap("status_code:400".getBytes()));
                    return;
                }
                communicator.send(ByteBuffer.wrap("status_code:200".getBytes()));
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

        public void addUDPConnection(int clientPort) throws IOException {//TODO error handling
            this.communicationManger.addUDPConnection(clientPort);
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
                GatewayServer.this.handle(byteBuffer, communicator);
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

            public void addUDPConnection(int UDPClientPort) throws IOException {//TODO error handling
                DatagramChannel udpServerSocket = DatagramChannel.open();
                udpServerSocket.configureBlocking(false);
                udpServerSocket.bind(new InetSocketAddress("localhost", UDPClientPort));
                udpServerSocket.register(selector, SelectionKey.OP_READ);
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
                                    SelectableChannel channel = key.channel();
                                    if (channel instanceof SocketChannel) { // TCP

                                        TCPCommunicator tcpCommunicator = (TCPCommunicator) key.attachment();
                                        MultiProtocolServer.this.messageManager.handle(tcpCommunicator);
                                    } else { // UDP

                                        DatagramChannel datagramChannel = (DatagramChannel) channel;
                                        UDPCommunicator udpCommunicator = new UDPCommunicator(datagramChannel);
                                        MultiProtocolServer.this.messageManager.handle(udpCommunicator);

                                    }
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
                        buffer.limit(buffer.array().length);
                        while (buffer.hasRemaining()) {
                            clientSocketChannel.write(buffer);
                        }

                        System.out.println("Sending massage to " + clientSocketChannel);
                    } catch (IOException e) {//TODO error handling
                        throw new RuntimeException(e);
                    }
                }


            }

            private class UDPCommunicator implements Communicator {

                private final DatagramChannel clientDatagramChannel;
                private SocketAddress clientAddress; //?

                public UDPCommunicator(DatagramChannel clientDatagramChannel) {
                    this.clientDatagramChannel = clientDatagramChannel;
                }

                @Override
                public ByteBuffer receive() {
                    try {
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        this.clientAddress = clientDatagramChannel.receive(buffer);
                        buffer.flip();
                        return buffer;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void send(ByteBuffer buffer) {
                    try {
                        buffer.limit(buffer.array().length);
                        this.clientDatagramChannel.send(buffer, clientAddress);
                    } catch (IOException e) {//TODO error handling
                        throw new RuntimeException(e);
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
            while (GatewayServer.this.multiProtocolServer.communicationManger.isRunning.get()) {
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
            Function<String, Command> function = s -> () -> {
                Method method = cls.getMethod(this.methodName);
                System.out.println((String) method.invoke(cls.newInstance()));
            };

            GatewayServer.this.requestHandler.factory.add(cls.getName(), function);
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


    /*==================================================================================================*/
    /*==================================================================================================*/
    private static JsonObject parseStringToJson(String input) {
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();

        // Split the input string by '@'
        String[] keyValuePairs = input.split("//");

        for (String pair : keyValuePairs) {
            // Split each pair by ':'
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                // Add the key-value pair to the JsonObjectBuilder
                jsonBuilder.add(keyValue[0], keyValue[1]);
            } else {
                // Handle invalid format (e.g., missing value)
                System.err.println("Invalid format for pair: " + pair);
            }
        }

        return jsonBuilder.build();
    }

    private static boolean isValid(JsonObject data, String[] keys){
        for (String key : keys){
            if(!data.containsKey(key)) {
                return false;
            }
        }

        return true;
    }

    public static void main(String[] args) {
        GatewayServer gatewayServer = new GatewayServer();
        gatewayServer.start();
    }
}

