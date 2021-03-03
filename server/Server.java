package chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) {
        try (var serverSocket = new ServerSocket(1025)) {

            System.out.println("Server started!");

            var i  = 1;
            serverSocket.setSoTimeout(14000); // required to be able to check interruption, tests don't allow higher value
            while (!Thread.interrupted()) {
                var sesh = new Session("Client " + i++, serverSocket.accept());
                sesh.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Session extends Thread {
        private final String clientName;
        private final Socket socket;

        private Session(String clientName, Socket socket) {
            this.clientName = clientName;
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println(clientName + " connected!");

            try (var in = new DataInputStream(socket.getInputStream());
                 var out = new DataOutputStream(socket.getOutputStream())) {

                String s;
                while (!isInterrupted()
                        && !(s = in.readUTF()).equalsIgnoreCase("/exit")) {

                    var count = s.strip().split("\\s+").length;
                    var sending = "Count is " + count;

                    System.out.println(clientName + " sent: " + s);
                    System.out.println("Sent to " + clientName + ": " + sending);
                    out.writeUTF(sending);
                }
                System.out.println(clientName + " disconnected!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
