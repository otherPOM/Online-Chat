package chat.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class MessageLogger {
    static final Map<String, MessageLogger> loggers = new HashMap<>();

    final String client1;
    final String client2;
    private final Path folder;
    private final Path logFile;
    private long lineCounter;
    private long newLineCounterClient1;
    private long newLineCounterClient2;

    private MessageLogger(String first, String second) throws IOException {
        client1 = first;
        client2 = second;
        folder = Path.of(first + second);
        Files.createDirectories(folder);
        logFile = folder.resolve("log.txt");
        lineCounter = Files.exists(logFile) ? countLinesMatching(logFile, s -> true) : 0;
        newLineCounterClient1 = Files.exists(folder.resolve(client1 + ".txt")) ?
                countLinesMatching(folder.resolve(client1 + ".txt"), s -> true) : 0;
        newLineCounterClient2 = Files.exists(folder.resolve(client2 + ".txt")) ?
                countLinesMatching(folder.resolve(client2 + ".txt"), s -> true) : 0;
        loggers.put(first + second, this);
    }

    static synchronized MessageLogger loggerFor(String client1, String client2) throws IOException {
        var first = client1.compareTo(client2) <= 0 ? client1 : client2;
        var second = first.equals(client1) ? client2 : client1;
        var l = loggers.get(first + second);
        return l == null ? new MessageLogger(first, second) : l;
    }

    private static long countLinesMatching(Path file, Predicate<String> predicate) throws IOException {
        try (var lines = Files.lines(file)) {
            return lines.filter(predicate).count();
        }
    }

    long getNewMessagesCountForClient(String clientName) {
        if (!clientName.equals(client1) && !clientName.equals(client2)) {
            return 0;
        }
        return clientName.equals(client1) ? newLineCounterClient1 : newLineCounterClient2;
    }

    void log(String message) throws IOException {
        synchronized (logFile) {
            try (var writer = Files.newBufferedWriter(logFile, CREATE, WRITE, APPEND)) {
                writer.write(message + System.lineSeparator());
                lineCounter++;
            }
        }
    }

    void logNew(String message, String receiverName) throws IOException {
        synchronized (receiverName.equals(client1) ? client1 : client2) {
            try (var writer = Files.newBufferedWriter(folder.resolve(receiverName + ".txt"),
                    CREATE, WRITE, APPEND)) {
                writer.write("(new) " + message + System.lineSeparator());
                if (receiverName.equals(client1)) {
                    newLineCounterClient1++;
                } else {
                    newLineCounterClient2++;
                }
            }
        }
    }

    synchronized String getLastTen(String receiverName) throws IOException {
        if (Files.exists(logFile)) {
            var sb = new StringBuilder();
            var nLines = receiverName.equals(client1) ? newLineCounterClient1 : newLineCounterClient2;

            if (nLines > 0) {
                var path = folder.resolve(receiverName.equals(client1) ? client1 + ".txt" : client2 + ".txt");

                try (var lines = Files.lines(logFile);
                     var newLines = Files.lines(path)) {
                    if (nLines <= 15) {
                        sb.append(lines
                                .skip(Math.max(lineCounter - nLines - 10, 0))
                                .limit(Math.min(10, lineCounter - nLines))
                                .collect(Collectors.joining(System.lineSeparator())));
                        sb.append(System.lineSeparator());
                        sb.append(newLines
                                .collect(Collectors.joining(System.lineSeparator())));
                    } else {
                        sb.append(lines
                                .skip(lineCounter - 25)
                                .limit(Math.max(0, 25 - nLines))
                                .collect(Collectors.joining(System.lineSeparator())));
                        sb.append(System.lineSeparator());
                        sb.append(newLines
                                .skip(Math.max(0, nLines - 25))
                                .collect(Collectors.joining(System.lineSeparator())));
                    }
                }

                // clear new messages log file
                try (var ignored = Files.newBufferedWriter(path,
                        CREATE, WRITE, TRUNCATE_EXISTING)) {
                    if (receiverName.equals(client1)) {
                        newLineCounterClient1 = 0;
                    } else {
                        newLineCounterClient2 = 0;
                    }
                }

                return sb.toString();
            } else {
                return getLastN(10);
            }
        }
        return null;
    }

    String getLastN(long n) throws IOException {
        if (Files.exists(logFile) && n > 0) {
            var start = Math.max(lineCounter - n, 0);
            var end = Math.min(25, lineCounter - start);

            synchronized (logFile) {
                try (var lines = Files.lines(logFile)) {
                    return lines
                            .skip(start)
                            .limit(end)
                            .collect(Collectors.joining(System.lineSeparator()));
                }
            }
        }
        return null;
    }

    String stats(String requesterName) throws IOException {
        var other = requesterName.equals(client1) ? client2 : client1;

        synchronized (logFile) {
            var yourMessagesC = countLinesMatching(logFile, line -> line.startsWith(requesterName));
            var otherMessagesC = countLinesMatching(logFile, line -> line.startsWith(other));
            return String.format("Server:\n" +
                            "Statistics with %s:\n" +
                            "Total messages: %d\n" +
                            "Messages from %s: %d\n" +
                            "Messages from %s: %d", other, lineCounter,
                    requesterName, yourMessagesC,
                    other, otherMessagesC);
        }
    }
}
