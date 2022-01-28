import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.vk.api.sdk.client.actors.Actor;
import com.vk.api.sdk.client.actors.UserActor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

public class ClientDeserializer implements JsonDeserializer<Client> {
    @Override
    public Client deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Client client = new Client();
        //main account
        JsonObject mainAccountJson = json.getAsJsonObject().get("mainAccount").getAsJsonObject();
        client.mainAccount = new UserActor(
                mainAccountJson.get("id").getAsInt(),
                mainAccountJson.get("accessToken").getAsString()
        );
        //accounts
        JsonArray accountsJson = json.getAsJsonObject().get("accounts").getAsJsonArray();
        for(JsonElement accountJsonElem:accountsJson){
            JsonObject accountJson = accountJsonElem.getAsJsonObject();
            client.accounts.put(accountJson.get("id").getAsInt(), new UserActor(
                    accountJson.get("id").getAsInt(),
                    accountJson.get("accessToken").getAsString())
            );
        }
        //redirects
        Gson gson = new Gson();
        JsonArray redirectsJson = json.getAsJsonObject().get("redirects").getAsJsonArray();
        for(JsonElement redirectJson:redirectsJson){
            Type type = new TypeToken<ArrayList<Redirect>>() {}.getType();
            ArrayList<Redirect> redirects = gson.fromJson(redirectJson,type);
            if(redirects.size()>0)
            client.redirects.put(redirects.get(0).fromChatId,redirects);
        }
        return client;
    }
}
