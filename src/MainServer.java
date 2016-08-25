import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 * @author DOS
 *
 */
class MainServer {

    public static void main(String[] args) throws Throwable {
        new MainServer(new InetSocketAddress("localhost", 6066));
    }

    ServerSocketChannel serverChannel;
    Selector selector;
    SelectionKey serverKey;

    MainServer(InetSocketAddress listenAddress) throws Throwable {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverKey = serverChannel.register(selector = Selector.open(), SelectionKey.OP_ACCEPT);
        serverChannel.bind(listenAddress);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                loop();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    static HashMap<SelectionKey, ClientSession> clientMap = new HashMap<SelectionKey, ClientSession>();

    void loop() throws Throwable {
        selector.selectNow();

        for (SelectionKey key : selector.selectedKeys()) {
            try {
                if (!key.isValid())
                    continue;

                if (key == serverKey) {
                    SocketChannel acceptedChannel = serverChannel.accept();

                    if (acceptedChannel == null)
                        continue;

                    acceptedChannel.configureBlocking(false);
                    SelectionKey readKey = acceptedChannel.register(selector, SelectionKey.OP_READ);
                    clientMap.put(readKey, new ClientSession(readKey, acceptedChannel));

                    System.out.println("New client ip=" + acceptedChannel.getRemoteAddress() + ", total clients=" + MainServer.clientMap.size());
                }

                if (key.isReadable()) {
                    ClientSession sesh = clientMap.get(key);

                    if (sesh == null)
                        continue;

                    sesh.input();
                }

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        selector.selectedKeys().clear();
    }

}