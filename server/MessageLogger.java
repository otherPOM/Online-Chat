package chat.server;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public class MessageLogger {
    private static final Map<String, SoftReference<MessageLogger>> loggers = new HashMap<>();

    private final String client1;
    private final String client2;
    private final Path folder;
    private final Path logFile;
    private int lineCounter;
    private int newLineCounterClient1;
    private int newLineCounterClient2;

    private MessageLogger(String first, String second) throws IOException {
        client1 = first;
        client2 = second;
        folder = Path.of(first + second);
        Files.createDirectories(folder);
        logFile = folder.resolve("log.txt");
        lineCounter = 0;
        newLineCounterClient1 = 0;
        newLineCounterClient2 = 0;
        loggers.put(first + second, new SoftReference<>(this));
    }

    static synchronized MessageLogger loggerFor(String client1, String client2) throws IOException {
        var first = client1.compareTo(client2) <= 0 ? client1 : client2;
        var second = first.equals(client1) ? client2 : client1;
        if (loggers.containsKey(first + second)) {
            var l = loggers.get(first + second).get();
            return l == null ? new MessageLogger(first, second) : l;
        } else {
            return new MessageLogger(first, second);
        }
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
                writer.write(message);
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
                try (var reader = Files.newBufferedReader(path)) {
                    if (nLines < 10) {
                        try (var logReader = Files.newBufferedReader(logFile)) {
                            logReader.lines()
                                    .skip(Math.max(0, lineCounter - 10))
                                    .limit(Math.min(lineCounter, 10) - nLines)
                                    .forEach(line -> sb.append(line).append(System.lineSeparator()));
                        }
                    }
                    reader.lines()
                            .skip(Math.max(0, nLines - 10))
                            .forEach(line -> sb.append("(new) ").append(line).append(System.lineSeparator()));
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
                try (var reader = Files.newBufferedReader(logFile)) {
                    return reader.lines()
                            .skip(Math.max(0, lineCounter - 10))
                            .collect(Collectors.joining(System.lineSeparator()));
                }
            }
        }
        return "";
    }
}
