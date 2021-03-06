package chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;

public class Server {
    private static final Queue<String> lastTenMessages = new LinkedBlockingQueue<>(10) {
        @Override
        public boolean offer(String s) {
            while (!super.offer(s)) {
                remove();
            }
            return true;
        }
    };

    private static final ConcurrentLinkedQueue<Session> sessions = new ConcurrentLinkedQueue<>();

    public static void main(String[] args) {
        try (var serverSocket = new ServerSocket(1025)) {
            serverSocket.setSoTimeout(14000); // required to be able to check interruption, tests don't allow higher value

            while (!Thread.interrupted()) {
                var sesh = new Session(serverSocket.accept());
                sessions.add(sesh);
                sesh.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Session extends Thread {
        private final DataOutputStream out;
        private final DataInputStream in;

        private String clientName = "";

        private Session(Socket socket) throws IOException {
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            try (in; out) {
                initName();

                for (String message : lastTenMessages) {
                    out.writeUTF(message);
                }

                while (!isInterrupted()) {
                    var mes = in.readUTF();
                    if (mes.equalsIgnoreCase("/exit")) {
                        out.writeUTF(mes);
                        sessions.remove(this);
                        // clientName disconnected ?
                        break;
                    }
                    mes = clientName + ": " + mes;
                    lastTenMessages.offer(mes);
                    for (Session session : sessions) {
                        session.send(mes);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getClientName() {
            return clientName;
        }

        private void initName() throws IOException {
            out.writeUTF("Server: write your name");
            var name = in.readUTF();
            while (nameIsTaken(name) || name.isBlank()) {
                out.writeUTF("Server: this name is already taken! Choose another one.");
                name = in.readUTF();
            }
            clientName = name;
            out.writeUTF("success");
        }

        private boolean nameIsTaken(String name) {
            return sessions.stream()
                    .map(Session::getClientName)
                    .filter(Predicate.not(String::isEmpty))
                    .anyMatch(seshName -> seshName.equals(name));
        }

        private void send(String message) throws IOException {
            if (!clientName.isEmpty()) // Only send to initialized clients
            out.writeUTF(message);
        }
    }
}
