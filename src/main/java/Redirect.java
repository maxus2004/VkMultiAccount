public class Redirect {
    int fromChatId;
    int fromUserId;
    int toChatId;
    int toUserId;
    //0 - my messages only, copy and send
    //1 - all messages, forward messages
    int forwardMode;

    public Redirect(int fromChatId, int fromUserId, int toChatId, int toUserId, int forwardMode) {
        this.fromChatId = fromChatId;
        this.toChatId = toChatId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.forwardMode = forwardMode;
    }
}