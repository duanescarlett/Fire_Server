import model.UserMod;

import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 *
 * @author DOS
 *
 */
class ClientSession {

    SelectionKey selkey;
    SocketChannel chan;
    ByteBuffer buf;
    UserMod user;
    String username;
    String clientMessageString = "";
    ResultSet resultSet;

    ClientSession(SelectionKey selkey, SocketChannel chan) throws Throwable {
        this.user = new UserMod();
        this.selkey = selkey;
        this.chan = (SocketChannel) chan.configureBlocking(false); // asynchronous/non-blocking
        buf = ByteBuffer.allocateDirect(1024); // 1024 byte capacity
    }

    private void disconnect() {
        MainServer.clientMap.remove(selkey);
        try {
            if (selkey != null)
                selkey.cancel();

            if (chan == null)
                return;

            System.out.println("bye bye " + (InetSocketAddress) chan.getRemoteAddress());
            chan.close();
        } catch (Throwable t) { /** quietly ignore  */ }
    }

    protected void input() {
        try {
            int amount_read = -1;

            try { amount_read = chan.read((ByteBuffer) buf.clear());
            } catch (Throwable t) { }

            if (amount_read == -1)
                disconnect();

            if (amount_read < 1)
                return; // if zero

            System.out.println("sending back " + buf.position() + " bytes");

            // turn this bus right around and send it back!
//            buf.flip();
//            chan.write(buf);
            //output("This test was passed");
            buf.flip();
            byte[] bytes = new byte[buf.limit()];
            buf.get(bytes);
            String str = new String(bytes);
            parser(str);
            buf.clear();
        } catch (Throwable t) {
            disconnect();
            t.printStackTrace();
        }
    }

    protected void output(String s) throws IOException {
        ByteBuffer buffy = ByteBuffer.wrap(s.getBytes());
        this.chan.write(buffy);
        buffy.clear();
    }

    private void parser(String s) {
        System.out.println("We are inside the parser and holding");
        System.out.println("(ClientSession.java): -> string for parsing " + s);

        String[] stringPeices = s.split(":");
        System.out.println(stringPeices[0]);


        if (stringPeices[0].equals("Login")) {
            System.out.println(stringPeices[1]);

            resultSet = this.user.get(stringPeices[1]);

            try {
                resultSet.last();
                this.username = resultSet.getString("username");
                resultSet.close();

                ArrayList allUsers = this.user.allUsers();

                output("Username:" + this.username);
                System.out.println("(MainServer.java): _> " + this.username);
                //Object[] arr = allUsers.toArray();

                for(int i = 0; i < allUsers.size(); i++){ // Send user one by one to client from array list
                    output("UserList:" + allUsers.get(i).toString());
                    System.out.println("UserList:" + allUsers.get(i).toString());

                    allUsers.remove(i);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (stringPeices[0].equals("Sign Up")) {

            if (this.user.insert(stringPeices[1])) {
                this.clientMessageString = "true";
            } else {
                this.clientMessageString = "false";
            }

        }
        else if (stringPeices[0].equals("Chat")) {
            System.out.println("\n(ClientSession.java): This is a chat message -> " + stringPeices[1]);
            System.out.println("(ClientSession.java): _> I am so happy the this works");
            System.out.println("(ClientSession.java): _> now for DB insert");
            try {
                output("Chat:" + stringPeices[1]);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        else if (stringPeices[0].equals("Update")) {
            this.user.insert(stringPeices[1]);
        }
        else {
            System.out.println("\n(ClientSession.java): I need to get the string parsing right");
        }
    }

}