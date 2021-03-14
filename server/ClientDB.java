package chat.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class ClientDB {
    private ClientDB() {
    }

    private static final Path PATH;
    private static final Path MODS;
    private static final Path KICKED;

    private static final String adminPW = "12345678"; // xd

    static {
        PATH = Path.of("Clients.txt");
        MODS = Path.of("Mods.txt");
        KICKED = Path.of("Kicked.txt");
        try {
            if (Files.notExists(PATH)) {
                Files.createFile(PATH);
            }
            if (Files.notExists(MODS)) {
                Files.createFile(MODS);
            }
            if (Files.notExists(KICKED)) {
                Files.createFile(KICKED);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String register(String login, String pw) throws IOException {
        synchronized (PATH) {
            try (var out = Files.newBufferedWriter(PATH, WRITE, APPEND);
                 var lines = Files.lines(PATH)) {
                if (lines.anyMatch(line -> line.startsWith(login))) {
                    return "Server: this login is already taken! Choose another one.";
                }

                if (pw.length() < 8) {
                    return "Server: the password is too short!";
                }

                out.write(login + ", " + pw.hashCode() + System.lineSeparator());
                var sesh = (Server.Session) Thread.currentThread(); // is that okay? idk
                sesh.authorized = true;
                sesh.clientName = login;
                return "Server: you are registered successfully!";
            }
        }
    }

    static String auth(String login, String pw) throws IOException {
        if (login.equalsIgnoreCase("admin") && pw.equals(adminPW)) {
            var sesh = (Server.Session) Thread.currentThread(); // is that okay? idk
            sesh.authorized = true;
            sesh.clientName = login;
            return "Server: you are authorized successfully!";
        }
        if (isKicked(login)) {
            return "Server: you are banned!";
        }
        synchronized (PATH) {
            try (var lines = Files.lines(PATH)) {
                var optional = lines
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

                var sesh = (Server.Session) Thread.currentThread(); // is that okay? idk
                sesh.authorized = true;
                sesh.clientName = login;
                return "Server: you are authorized successfully!";
            }
        }
    }

    static void grantMod(String login) throws IOException {
        synchronized (MODS) {
            try (var out = Files.newBufferedWriter(MODS, WRITE, APPEND)) {
                out.write(login + System.lineSeparator());
            }
        }
    }

    static void revokeMod(String login) throws IOException {
        synchronized (MODS) {
            try (var lines = Files.lines(MODS)) {
                var list = lines.filter(line -> !line.equals(login))
                        .collect(Collectors.toList());
                try (var writer = Files.newBufferedWriter(MODS, WRITE, TRUNCATE_EXISTING)) {
                    list.forEach(str -> {
                        try {
                            writer.write(str);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }
    }

    static boolean isMod(String login) throws IOException {
        synchronized (MODS) {
            try (var lines = Files.lines(MODS)) {
                return lines.anyMatch(line -> line.equals(login));
            }
        }
    }

    static boolean isKicked(String login) throws IOException {
        synchronized (KICKED) {
            try (var lines = Files.lines(KICKED)) {
                return lines.anyMatch(line -> line.equals(login));
            }
        }
    }

    static void kick(String login) {
        new Kicker(login).start();
    }

    static class Kicker extends Thread {
        private final String login;

        public Kicker(String login) {
            this.login = login;
        }

        @Override
        public void run() {
            try {
                synchronized (KICKED) {
                    try (var writer = Files.newBufferedWriter(KICKED, WRITE, APPEND)) {
                        writer.write(login);
                    }
                }
                sleep(300000);
                synchronized (KICKED) {
                    try (var lines = Files.lines(KICKED)) {
                        var list = lines.filter(line -> !line.equals(login))
                                .collect(Collectors.toList());
                        try (var writer = Files.newBufferedWriter(KICKED, WRITE, TRUNCATE_EXISTING)) {
                            list.forEach(line -> {
                                try {
                                    writer.write(line);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

