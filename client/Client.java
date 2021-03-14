package chat.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Client() {}

    public static void main(String[] args) {

        try (var socket = new Socket("127.0.0.1", 1025);
             var in = new DataInputStream(socket.getInputStream());
             var out = new DataOutputStream(socket.getOutputStream());
             var scan = new Scanner(System.in)) {

            System.out.println("Client started!");

            var listeningThread = new Thread(() -> {
                try {
                    String s;
                    while (!(s = in.readUTF()).equalsIgnoreCase("/exit") ) {
                        System.out.println(s);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            var sendingThread = new Thread(() -> {
                try {
                    while (true) {
                        var s = scan.nextLine();
                        out.writeUTF(s);
                        if (s.equalsIgnoreCase("/exit")) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            listeningThread.start();
            sendingThread.start();

            sendingThread.join();
            listeningThread.join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
