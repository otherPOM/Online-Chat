package chat.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

public class ClientDB {
    private static final Path PATH;

    static {
        PATH = Path.of("Clients.txt");
        try {
            if (Files.notExists(PATH)) {
                Files.createFile(PATH);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static synchronized String register(String login, String pw) throws IOException {
        try (var out = Files.newBufferedWriter(PATH, CREATE, WRITE, APPEND);
             var in = Files.newBufferedReader(PATH)) {

            if (in.lines().anyMatch(line -> line.startsWith(login))) {
                return "Server: this login is already taken! Choose another one.";
            }

            if (pw.length() < 8) {
                return "Server: the password is too short!";
            }

            out.write(login + ", " + pw.hashCode() + System.lineSeparator());
            var sesh = ((Server.Session) Thread.currentThread());
            sesh.authorized = true; // is that okay? idk
            sesh.clientName = login;
            return "Server: you are registered successfully!";
        }
    }

    static synchronized String auth(String login, String pw) throws IOException {
        try (var in = Files.newBufferedReader(PATH)) {

            var optional = in.lines()
                    .filter(line -> line.startsWith(login))
                    .findFirst();
            if (optional.isEmpty()) {
                return "Server: incorrect login!";
            }

            var creds = optional.get().split(", ");
            if (!login.equals(creds[0])) {
                return "Server: incorrect login!";
            }
            if (pw.hashCode() != Integer.parseInt(creds[1])) {
                return "Server: incorrect password!";
            }

            var sesh = ((Server.Session) Thread.currentThread());
            sesh.authorized = true; // is that okay? idk
            sesh.clientName = login;
            return "Server: you are authorized successfully!";
        }
    }
}

