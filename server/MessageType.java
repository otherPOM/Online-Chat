package chat.server;

import java.util.Locale;

public enum MessageType {
    MESSAGE, REGISTRATION, AUTH, LIST, CHAT, EXIT, INVALID;

    public static MessageType messageType(String message) {
        message = message.toLowerCase(Locale.ROOT);
        return !message.startsWith("/") ? MessageType.MESSAGE :
                message.startsWith("/registration") ? MessageType.REGISTRATION :
                        message.startsWith("/auth") ? MessageType.AUTH :
                                message.startsWith("/list") ? MessageType.LIST :
                                        message.startsWith("/chat") ? MessageType.CHAT :
                                                message.startsWith("/exit") ? MessageType.EXIT : MessageType.INVALID;
    }
}
