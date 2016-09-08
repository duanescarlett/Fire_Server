import model.UserMod;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

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

            buf.flip();
            byte[] bytes = new byte[buf.limit()];
            buf.get(bytes);
            String str = new String(bytes);
            parser(str);
            buf.clear();
        }
        catch (Throwable t) {
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
                ArrayList allNotifications = this.user.allNotifications();

                output("Username:" + this.username);
                System.out.println("(MainServer.java): _> " + this.username);
                //Object[] arr = allUsers.toArray();

                for(int i = 0; i < allUsers.size(); i++){ // Send user one by one to client from array list
                    output("UserList:" + allUsers.get(i).toString());
                    System.out.println("UserList:" + allUsers.get(i).toString());
                    Thread.sleep(100);
                    //wait(100);
                    //allUsers.remove(i);
                }
                allUsers.clear();

                for(int i = 0; i < allNotifications.size(); i++){
                    System.out.println("Notification:" + allNotifications.get(i).toString());
                    output("Notification:" + allNotifications.get(i).toString());

                    Thread.sleep(100);
                    //wait(100);
                    //allNotifications.remove(i);
                }
                allNotifications.clear();

                ArrayList allMessages = this.user.allMessages(this.username);

                for(int i = 0; i < allMessages.size(); ++i){ // Send user one by one to client from array list
                    try {
                        output("Messages:" + allMessages.get(i).toString());
                        Thread.sleep(100);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Messages:" + allMessages.get(i).toString());

                    allMessages.remove(i);
                }
                allMessages.clear();

            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
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
            Date date = new Date();

            String[] st = stringPeices[1].split(":");

            this.user.insert("INSERT INTO fire_brigade.message (datetime, message, sender, receiver, seen) VALUES('"+date.toString()+"', '"+stringPeices[1]+"', 'fireman', '"+st[1]+"', 'no')") ;
            try {
                output("Chat:" + stringPeices[1]);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        else if (stringPeices[0].equals("Update")) {
            this.user.insert(stringPeices[1]);
        }
        else if (stringPeices[0].equals("Notification")){
            this.user.insert(stringPeices[1]);
        }
        else if (stringPeices[0].equals("Messages")){
            ArrayList allMessages = this.user.allMessages(this.username);

            for(int i = 0; i < allMessages.size(); ++i){ // Send user one by one to client from array list
                try {
                    output("Messages:" + allMessages.get(i).toString());
                    wait(100);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Messages:" + allMessages.get(i).toString());

                allMessages.remove(i);
            }
            allMessages.clear();
        }

    }

}