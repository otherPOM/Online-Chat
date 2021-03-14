package chat.server;

import java.util.Locale;

public enum MessageType {
    MESSAGE,
    REGISTRATION,
    AUTH,
    LIST,
    CHAT,
    EXIT,
    INVALID,
    KICK,
    GRANT,
    REVOKE,
    UNREAD,
    HISTORY,
    STATS;

    public static MessageType messageType(String message) {
        message = message.toLowerCase(Locale.ROOT);
        return !message.startsWith("/") ? MessageType.MESSAGE :
                message.startsWith("/registration") ? MessageType.REGISTRATION :
                message.startsWith("/auth") ? MessageType.AUTH :
                message.startsWith("/list") ? MessageType.LIST :
                message.startsWith("/chat") ? MessageType.CHAT :
                message.startsWith("/exit") ? MessageType.EXIT :
                message.startsWith("/kick") ? KICK :
                message.startsWith("/grant") ? GRANT :
                message.startsWith("/revoke") ? REVOKE :
                message.startsWith("/unread") ? UNREAD :
                message.startsWith("/history") ? HISTORY :
                message.startsWith("/stats") ? STATS : MessageType.INVALID;
    }
}
