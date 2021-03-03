package chat.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {

        try (var socket = new Socket("127.0.0.1", 1025);
             var in = new DataInputStream(socket.getInputStream());
             var out = new DataOutputStream(socket.getOutputStream());
             var scan = new Scanner(System.in)) {

            System.out.println("Client started!");

            while (!Thread.interrupted()) {
                var s = scan.nextLine();
                out.writeUTF(s);
                if (s.equalsIgnoreCase("/exit")) {
                    break;
                } else {
                    System.out.println(in.readUTF());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
