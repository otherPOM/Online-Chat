package chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server {
    private Server() {}

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

    private static String listOnlineUsers(String exclude) {
        var sb = new StringBuilder("Server: ");

        if (sessions.size() == 1) {
            sb.append("no one online");
            return sb.toString();
        }
        sb.append("online:");
        sessions.stream()
                .filter(session -> !session.clientName.equals(exclude))
                .forEach(session -> sb.append(" ").append(session.clientName));
        return sb.toString();
    }




    static class Session extends Thread {
        private final DataOutputStream out;
        private final DataInputStream in;

        String clientName;
        boolean authorized = false;
        Session addressee;

        private MessageLogger logger;

        private Session(Socket socket) throws IOException {
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
        }

//        TODO decompose the method?
        @Override
        public void run() {
            try (in; out) {
                out.writeUTF("Server: authorize or register");

                mainLoop:
                while (!isInterrupted()) {
                    var clientMessage = in.readUTF().strip();

                    switch (MessageType.messageType(clientMessage)) {
                        case REGISTRATION:
                            var regCreds = clientMessage.split("\\s+");
                            out.writeUTF(ClientDB.register(regCreds[1], regCreds[2]));
                            break;
                        case AUTH:
                            var authCreds = clientMessage.split("\\s+");
                            out.writeUTF(ClientDB.auth(authCreds[1], authCreds[2]));
                            break;
                        case LIST:
                            if (!authorized) {
                                out.writeUTF("Server: you are not in the chat!");
                                break;
                            }
                            out.writeUTF(listOnlineUsers(clientName));
                            break;
                        case CHAT:
                            if (!authorized) {
                                out.writeUTF("Server: you are not in the chat!");
                                break;
                            }
                            var addresseeName = clientMessage.split("\\s+")[1];
                            if (addresseeName.equals(clientName)) { // safety precaution
                                break;
                            }
                            var optionalAddressee = sessions.stream()
                                    .filter(session -> session.clientName.equals(addresseeName))
                                    .findFirst();
                            if (optionalAddressee.isEmpty()) {
                                out.writeUTF("Server: the user is not online!");
                            } else {
                                addressee = optionalAddressee.get();
                                logger = MessageLogger.loggerFor(clientName, addressee.clientName);
                                out.writeUTF(logger.getLastTen(clientName));
                            }
                            break;
                        case MESSAGE:
                            if (!authorized) {
                                out.writeUTF("Server: you are not in the chat!");
                                break;
                            }
                            if (addressee == null) {
                                out.writeUTF("Server: use /list command to choose a user to text!");
                                break;
                            }
                            clientMessage = clientName + ": " + clientMessage;
                            out.writeUTF(clientMessage);
                            if (addressee.addressee == this) {
                                addressee.send(clientMessage);
                            } else {
                                logger.logNew(clientMessage, addressee.clientName);
                            }
                            logger.log(clientMessage);
                            break;
                        case INVALID:
                            out.writeUTF("Server: incorrect command!");
                            break;
                        case EXIT:
                            out.writeUTF("/exit");
                            sessions.forEach(session -> session.disconnect(this));
                            sessions.remove(this);
                            break mainLoop;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void send(String message) throws IOException {
            out.writeUTF(message);
        }

        private void disconnect(Session other) {
            if (addressee == other) {
                addressee = null;
            }
        }
    }
}
