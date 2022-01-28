import com.google.gson.*;
import com.vk.api.sdk.client.actors.Actor;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class ClientSerializer implements JsonSerializer<Client> {
    @Override
    public JsonElement serialize(Client src, Type typeOfSrc, JsonSerializationContext context) {
        //mainAccount
        JsonObject mainAccountJson = new JsonObject();
        mainAccountJson.addProperty("id",src.mainAccount.getId());
        mainAccountJson.addProperty("accessToken",src.mainAccount.getAccessToken());
        //accounts
        JsonArray accountsJson = new JsonArray();
        for(Actor account:src.accounts.values()){
            JsonObject accountJson = new JsonObject();
            accountJson.addProperty("id",account.getId());
            accountJson.addProperty("accessToken",account.getAccessToken());
            accountsJson.add(accountJson);
        }
        //redirects
        JsonArray redirectsJson = new JsonArray();
        Gson gson = new Gson();
        for(ArrayList<Redirect> redirects:src.redirects.values()){
            JsonElement redirectJson = gson.toJsonTree(redirects);
            redirectsJson.add(redirectJson);
        }
        //client
        JsonObject json = new JsonObject();
        json.add("mainAccount",mainAccountJson);
        json.add("accounts",accountsJson);
        json.add("redirects",redirectsJson);
        return json;
    }
}
