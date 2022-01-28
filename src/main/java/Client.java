import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.messages.responses.GetLongPollServerResponse;

import java.util.ArrayList;
import java.util.HashMap;

public class Client {
    public UserActor mainAccount;
    public HashMap<Integer, UserActor> accounts = new HashMap<>();
    public HashMap<Integer, ArrayList<Redirect>> redirects = new HashMap<>();
    TransportClient transportClient = new HttpTransportClient();
    VkApiClient vk = new VkApiClient(transportClient);

    public void startUpdating() {
        new Thread(this::updateLoop).start();
    }

    int randomId() {
        return (int) (Math.random() * 1000000000);
    }

    void updateLoop() {

        try {
            GetLongPollServerResponse longPollParams = vk.messages().getLongPollServer(mainAccount).execute();
            String server = "https://" + longPollParams.getServer();
            String ts = "" + longPollParams.getTs();
            String key = "" + longPollParams.getKey();
            while (true) {
                String eventsStr;
                try {
                    eventsStr = vk.longPoll().
                            getEvents(
                                    server,
                                    key,
                                    ts).
                            waitTime(25).
                            unsafeParam("version", 3).
                            unsafeParam("mode", 2 + 8).
                            executeAsString();
                } catch (ClientException clientE) {
                    clientE.printStackTrace();
                    continue;
                }
                JsonObject response = new JsonParser().parse(eventsStr).getAsJsonObject();
                if (response.has("failed")) {
                    switch (response.get("failed").getAsInt()) {
                        case 1 -> ts = response.get("ts").getAsString();
                        case 2 -> {
                            longPollParams = vk.messages().getLongPollServer(mainAccount).execute();
                            longPollParams = vk.messages().getLongPollServer(mainAccount).execute();
                            server = "https://" + longPollParams.getServer();
                            key = "" + longPollParams.getKey();
                        }
                        case 3 -> {
                            longPollParams = vk.messages().getLongPollServer(mainAccount).execute();
                            server = "https://" + longPollParams.getServer();
                            ts = "" + longPollParams.getTs();
                            key = "" + longPollParams.getKey();
                        }
                    }
                    continue;
                }
                ts = response.get("ts").getAsString();
                JsonArray updates = response.get("updates").getAsJsonArray();
                for (JsonElement update : updates) {
                    processUpdate(update.getAsJsonArray());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void processUpdate(JsonArray update) {
        if (update.get(0).getAsInt() == 4) {
            int messageId = update.get(1).getAsInt();
            int chatId = update.get(3).getAsInt();
            String text = update.get(5).getAsString();
            int fromId = chatId;
            if (update.get(6).getAsJsonObject().has("from")) {
                fromId = update.get(6).getAsJsonObject().get("from").getAsInt();
            }
            JsonObject attachments = update.get(7).getAsJsonObject();
            System.out.println(update);
            System.out.println("new message \"" + text + "\" with id " + messageId + " in " + chatId + " from " + fromId);

            if (text.startsWith("/register") && fromId == mainAccount.getId()) {
                try {
                    String[] params = text.split(" ");
                    if (params[1].equals("clear")) {
                        accounts.clear();
                        accounts.put(mainAccount.getId(),mainAccount);
                        vk.messages().delete(mainAccount).messageIds(messageId).deleteForAll(true).execute();
                        vk.messages().send(mainAccount).peerId(mainAccount.getId()).message("accounts removed").randomId(randomId()).execute();
                        return;
                    }
                    int id = Integer.parseInt(params[1]);
                    String token = params[2];
                    accounts.put(id,new UserActor(id,token));
                    vk.messages().delete(mainAccount).messageIds(messageId).deleteForAll(true).execute();
                    vk.messages().send(mainAccount).peerId(mainAccount.getId()).message("registered account").randomId(randomId()).execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (text.startsWith("/chatid") && fromId == mainAccount.getId()) {
                try {
                    vk.messages().delete(mainAccount).messageIds(messageId).deleteForAll(true).execute();
                    vk.messages().send(mainAccount).peerId(mainAccount.getId()).message("chat id: ").randomId(randomId()).execute();
                    vk.messages().send(mainAccount).peerId(mainAccount.getId()).message(mainAccount.getId() + ":" + chatId).randomId(randomId()).execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (text.startsWith("/redirect") && fromId == mainAccount.getId()) {
                try {
                    String[] params = text.split(" ");
                    if (params[1].equals("clear")) {
                        redirects.remove(chatId);
                        vk.messages().delete(mainAccount).messageIds(messageId).deleteForAll(true).execute();
                        vk.messages().send(mainAccount).peerId(mainAccount.getId()).message("redirects removed").randomId(randomId()).execute();
                        return;
                    }
                    if (params[1].equals("add")) {
                        String[] chatIdParams = params[2].split(":");
                        int mode = Integer.parseInt(params[3]);
                        int toUserId = Integer.parseInt(chatIdParams[0]);
                        int toChatId = Integer.parseInt(chatIdParams[1]);
                        int fromUserId = mainAccount.getId();
                        if(mode != 0 && mode != 1) return;
                        if(mode == 1 && toChatId!=chatId){
                            vk.messages().send(mainAccount).peerId(mainAccount.getId()).message("cant use redirect mode 1 when toUserId != fromUserId").randomId(randomId()).execute();
                            vk.messages().delete(mainAccount).messageIds(messageId).deleteForAll(true).execute();
                            return;
                        }
                        Redirect redirect = new Redirect(chatId, fromUserId, toChatId, toUserId, mode);
                        if (redirects.containsKey(chatId)) {
                            redirects.get(chatId).add(redirect);
                        } else {
                            ArrayList<Redirect> r = new ArrayList<>();
                            r.add(redirect);
                            redirects.put(chatId, r);
                        }
                        vk.messages().delete(mainAccount).messageIds(messageId).deleteForAll(true).execute();
                        vk.messages().send(mainAccount).peerId(mainAccount.getId()).message("redirect added").randomId(randomId()).execute();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (redirects.containsKey(chatId)) {
                for (Redirect redirect : redirects.get(chatId)) {
                    if(!accounts.containsKey(redirect.toUserId)){
                        try {
                            vk.messages().send(mainAccount).peerId(mainAccount.getId()).message("access_token not registered. Run command \"/register "+redirect.toUserId+" access_token\" get access_token here: https://oauth.vk.com/oauth/authorize?client_id=2685278&scope=messages,offline&response_type=token").randomId(randomId()).execute();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                    if (redirect.forwardMode == 0 && fromId == redirect.fromUserId) {
                        try {
                            vk.messages().send(accounts.get(redirect.toUserId)).message(text).peerId(redirect.toChatId).randomId(randomId()).execute();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (redirect.forwardMode == 1) {
                        try {
                            if (redirect.toUserId != redirect.fromUserId) {
                                System.err.println("cant use redirect mode 1 when toUserId != fromUserId");
                                return;
                            }
                            vk.messages().send(accounts.get(redirect.toUserId)).forwardMessages(messageId).peerId(redirect.toChatId).randomId(randomId()).execute();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
