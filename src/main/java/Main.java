
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;

public class Main {
    static Scanner consoleScanner = new Scanner(System.in);

    static HashMap<Integer, Client> clients;

    static boolean loadData(){
        try {
            String data = Files.readString(Paths.get("data.json"), StandardCharsets.UTF_8);
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Client.class, new ClientDeserializer())
                    .create();
            Type type = new TypeToken<HashMap<Integer, Client>>() {}.getType();
            clients = gson.fromJson(data, type);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    static void saveData(){
        try {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapter(Client.class, new ClientSerializer())
                    .create();
            Files.writeString(Paths.get("data.json"), gson.toJson(clients));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        boolean loaded = loadData();
        if(!loaded){
            System.out.print("generate new data.json? (y/n): ");
            String line = consoleScanner.nextLine();
            if (line.equals("y")) {
                try {
                    Files.writeString(Paths.get("data.json"), "{}");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            System.exit(0);
        }

        for (Client client : clients.values()) {
            client.startUpdating();
        }

        while(true){
            String line = consoleScanner.nextLine();
            switch (line) {
                case "stop" -> {
                    saveData();
                    System.exit(0);
                }
                case "save" -> saveData();
                case "help" -> {
                    System.out.println("stop - save data and stop the server");
                    System.out.println("save - save data");
                }
            }
        }
    }
}
