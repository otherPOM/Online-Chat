package chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class Server {
    private Server() {
    }

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

        var online = sessions.stream()
                .filter(session -> session.authorized &&
                        !session.clientName.equals(exclude))
                .map(Session::getClientName)
                .collect(Collectors.toList());

        if (online.isEmpty()) {
            sb.append("no one online");
            return sb.toString();
        }
        Collections.sort(online);
        sb.append("online:");
        online.forEach(seshName -> sb.append(" ").append(seshName));
        return sb.toString();
    }

    private static Session getSession(String name) {
        return sessions.stream()
                .filter(session -> Objects.equals(name, session.clientName))
                .findAny()
                .orElse(null);
    }

    private static void disconnect(Session session) {
        sessions.forEach(s -> s.addressee = s.addressee == session ? null : s.addressee);
        sessions.remove(session);
        session.authorized = false;
        session.clientName = null;
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

        public String getClientName() {
            return clientName;
        }

        @Override
        public void run() {
            try (in; out) {
                out.writeUTF("Server: authorize or register");

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
                            var optionalAddressee = getSession(addresseeName);
                            if (optionalAddressee == null) {
                                out.writeUTF("Server: the user is not online!");
                            } else {
                                addressee = optionalAddressee;
                                logger = MessageLogger.loggerFor(clientName, addressee.clientName);
                                var history = logger.getLastTen(clientName);
                                if (history != null) {
                                    out.writeUTF(history);
                                }
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
                        case KICK:
                            if (!authorized) {
                                out.writeUTF("Server: you are not in the chat!");
                                break;
                            }
                            var nameToBeKicked = clientMessage.split("\\s+")[1];
                            if (clientName.equalsIgnoreCase("admin")) {
                                if (nameToBeKicked.equalsIgnoreCase("admin")) {
                                    out.writeUTF("Server: you can't kick yourself!");
                                } else {
                                    ClientDB.kick(nameToBeKicked);
                                    var kicked = getSession(nameToBeKicked);
                                    kicked.send("Server: you have been kicked out of the server!");
                                    disconnect(kicked);
                                    out.writeUTF("Server: " + nameToBeKicked + " was kicked!");
                                }
                            } else if (ClientDB.isMod(clientName)) {
                                if (ClientDB.isMod(nameToBeKicked) || nameToBeKicked.equalsIgnoreCase("admin")) {
                                    out.writeUTF("Server: you can't kick mods or admin!");
                                } else {
                                    ClientDB.kick(nameToBeKicked);
                                    var kicked = getSession(nameToBeKicked);
                                    kicked.send("Server: you have been kicked out of the server!");
                                    disconnect(kicked);
                                    out.writeUTF("Server: " + nameToBeKicked + " was kicked!");
                                }
                            } else {
                                out.writeUTF("Server: you are not a moderator or an admin!");
                            }
                            break;
                        case GRANT:
                            if (!authorized) {
                                out.writeUTF("Server: you are not in the chat!");
                                break;
                            }
                            if (!clientName.equalsIgnoreCase("admin")) {
                                out.writeUTF("Server: you are not an admin!");
                            } else {
                                var nameToMod = clientMessage.split("\\s+")[1];
                                if (ClientDB.isMod(nameToMod)) {
                                    out.writeUTF("Server: this user is already a moderator!");
                                } else {
                                    ClientDB.grantMod(nameToMod);
                                    getSession(nameToMod).send("Server: you are the new moderator now!");
                                    out.writeUTF("Server: " + nameToMod + " is the new moderator!");
                                }
                            }
                            break;
                        case REVOKE:
                            if (!authorized) {
                                out.writeUTF("Server: you are not in the chat!");
                                break;
                            }
                            if (!clientName.equalsIgnoreCase("admin")) {
                                out.writeUTF("Server: you are not an admin!");
                            } else {
                                var nameToMod = clientMessage.split("\\s+")[1];
                                if (!ClientDB.isMod(nameToMod)) {
                                    out.writeUTF("Server: this user is not a moderator!");
                                } else {
                                    ClientDB.revokeMod(nameToMod);
                                    getSession(nameToMod).send("Server: you are no longer a moderator!");
                                    out.writeUTF("Server: " + nameToMod + " is no longer a moderator!");
                                }
                            }
                            break;
                        case STATS:
                            if (!authorized) {
                                out.writeUTF("Server: you are not in the chat!");
                                break;
                            }

                            out.writeUTF(logger.stats(clientName));
                            break;
                        case UNREAD:
                            if (!authorized) {
                                out.writeUTF("Server: you are not in the chat!");
                                break;
                            }

                            var unreadNames = MessageLogger.loggers.values().stream()
                                    .map(l -> {
                                        if (l.getNewMessagesCountForClient(clientName) > 0) {
                                            return clientName.equals(l.client1) ? l.client2 : l.client1;
                                        }
                                        return null;
                                    })
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .sorted()
                                    .collect(Collectors.joining(" "));
                            if (unreadNames.isBlank()) {
                                out.writeUTF("Server: no one unread");
                            } else {
                                out.writeUTF("Server: unread from: " + unreadNames);
                            }
                            break;
                        case HISTORY:
                            if (!authorized) {
                                out.writeUTF("Server: you are not in the chat!");
                                break;
                            }

                            if (addressee == null) {
                                out.writeUTF("Server: you are not in conversation!");
                                break;
                            }

                            var x = clientMessage.split("\\s+")[1];
                            if (!x.matches("[0-9]+")) {
                                out.writeUTF("Server: " + x + " is not a number!");
                            } else {
                                var history = logger.getLastN(Integer.parseInt(x));
                                if (history != null) {
                                    out.writeUTF("Server:" + System.lineSeparator() + history);
                                }
                            }
                            break;
                        case INVALID:
                            out.writeUTF("Server: incorrect command!");
                            break;
                        case EXIT:
                            out.writeUTF("/exit");
                            disconnect(this);
                            return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void send(String message) throws IOException {
            out.writeUTF(message);
        }
    }
}
