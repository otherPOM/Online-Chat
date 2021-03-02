package chat;

import java.util.Scanner;

public class Main {
    private static final Scanner scan = new Scanner(System.in);

    public static void main(String[] args) {
        while (scan.hasNextLine()) {
            var line = scan.nextLine();

            var name = line.substring(0, line.indexOf(' '));

            line = line.substring(line.indexOf(' ') + 1);
            var action = line.contains(" ") ?
                    line.substring(0, line.indexOf(' ')) : line;

            if (action.equalsIgnoreCase("sent")) {
                line = line.substring(line.indexOf(' ') + 1);
                System.out.println(name + ": " + line);
            }
        }
    }

    private static void print(String[] splitLine) {
        var sb = new StringBuilder();
        sb.append(splitLine[0]).append(": ");

        for (int i = 2; i < splitLine.length; i++) {
            sb.append(splitLine[i] == null ? ' ' : splitLine[i]);
        }
        System.out.println(sb);
    }
}
