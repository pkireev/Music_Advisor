package advisor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class Main {
    static final String granted = "Got the code. Return back to your program.";
    static final String denied = "Authorization code not found. Try again.";
    static boolean isLoggedIn = false;
    static HttpServer server = null;
    static HttpClient client = HttpClient.newBuilder().build();
    static String spotifyAddress = "https://accounts.spotify.com";
    static String spotifyAPI = "https://api.spotify.com";
    static String client_id = Constants.client_id;
    static String client_secret = Constants.client_secret;
    static String accessToken = "";
    static Map<String, String> categories = new HashMap<>();
    static int localPort = 8080;


    public static void main(String[] args) {

        for (int i = 0; i < args.length; i++) {
            if ("-access".equals(args[i])) {
                spotifyAddress = args[i + 1];
            }

            if ("-resource".equals(args[i])) {
                spotifyAPI = args[i + 1];
            }
        }

        loadCategories();

        Scanner sc = new Scanner(System.in);
        String result;
        String plist = "";
        String input;

        while (true) {
            input = sc.nextLine();

            if (input.contains("playlists")) {
                plist = input.replace("playlists ", "");
                input = "playlists";
            }

            switch (input) {
                case "featured":
                    result = getFeatured();
                    break;

                case "new":
                    result = getNew();
                    break;

                case "categories":
                    result = getCategories();
                    break;

                case "playlists":
                    result = getPlaylists(plist);
                    break;

                case "exit":
                    result = "---GOODBYE!---";
                    break;

                case "auth":
                    if (server == null) {
                        createServer();
                        createContext();
                    }

                    auth();
                    result = "waiting for code...";
                    break;

                default:
                    result = "";
            }

            if (!result.isEmpty()) {
                System.out.println(result);
            }

            if ("exit".equals(input)) {
                if (!(server == null)) {
                    server.stop(1);
                }
                break;
            }
        }
    }

    static String getFeatured() {
        pause();

        if (isLoggedIn) {
            String apiPath = spotifyAPI + "/v1/browse/featured-playlists";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .uri(URI.create(apiPath))
                    .GET()
                    .build();

            HttpResponse<String> response = null;

            try {
                response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (response != null) {
                JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray featuredArray = obj.getAsJsonObject("playlists").getAsJsonArray("items");

                String name;
                String url;

                for (JsonElement el : featuredArray) {
                    name = el.getAsJsonObject().get("name").getAsString();
                    url = el.getAsJsonObject().getAsJsonObject("external_urls").get("spotify").getAsString();

                    System.out.println(name);
                    System.out.println(url);
                    System.out.println();
                }
            }

            return "";
        } else {
            return "Please, provide access for application.";
        }
    }

    static String getNew() {
        pause();

        if (isLoggedIn) {
            String apiPath = spotifyAPI + "/v1/browse/new-releases";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .uri(URI.create(apiPath))
                    .GET()
                    .build();

            HttpResponse<String> response = null;

            try {
                response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (response != null) {
                JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray items = obj.getAsJsonObject("albums").getAsJsonArray("items");

                String name;
                String url;

                for (JsonElement el : items) {
                    List<String> artists = new ArrayList<>();

                    JsonArray artistsArray = el.getAsJsonObject().getAsJsonArray("artists");
                    for (JsonElement artistEl : artistsArray) {
                        artists.add(artistEl.getAsJsonObject().get("name").getAsString());
                    }

                    name = el.getAsJsonObject().get("name").getAsString();
                    url = el.getAsJsonObject().getAsJsonObject("external_urls").get("spotify").getAsString();

                    System.out.println(name);
                    System.out.println(artists);
                    System.out.println(url);
                    System.out.println();
                }
            }

            return "";
        } else {
            return "Please, provide access for application.";
        }
    }

    static String getCategories() {
        pause();

        if (isLoggedIn) {
            String apiPath = spotifyAPI + "/v1/browse/categories";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .uri(URI.create(apiPath))
                    .GET()
                    .build();

            HttpResponse<String> response = null;

            try {
                response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (response != null) {
                JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray featuredArray = obj.getAsJsonObject("categories").getAsJsonArray("items");

                String name;
                String id;

                for (JsonElement el : featuredArray) {
                    name = el.getAsJsonObject().get("name").getAsString();
                    id = el.getAsJsonObject().get("id").getAsString();

                    System.out.println(name);
                    categories.put(name, id);
                }

                saveCategories();
            }

            return  "";

        } else {
            return "Please, provide access for application.";
        }
    }

    static String getPlaylists(String plist) {
        pause();

        if (isLoggedIn) {
            String apiPath = String.format(spotifyAPI + "/v1/browse/categories/%s/playlists", categories.get(plist));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .uri(URI.create(apiPath))
                    .GET()
                    .build();

            HttpResponse<String> response = null;

            try {
                response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (response != null) {
                if (response.body().contains("error")) {
                    return "Test unpredictable error message"; //"Specified id doesn't exist";
                }

                if (response.statusCode() == 404) {
                    return "Specified id doesn't exist"; //"Unknown category name.";
                }

                JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray featuredArray = obj.getAsJsonObject("playlists").getAsJsonArray("items");

                String name;
                String url;

                for (JsonElement el : featuredArray) {
                    name = el.getAsJsonObject().get("name").getAsString();
                    url = el.getAsJsonObject().getAsJsonObject("external_urls").get("spotify").getAsString();

                    System.out.println(name);
                    System.out.println(url);
                    System.out.println();
                }
            }

            return "";
        } else {
            return "Please, provide access for application.";
        }
    }

    static void createServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(localPort), 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.start();
    }

    static void createContext() {

        server.createContext("/", exchange -> {
            String query = exchange.getRequestURI().getQuery();

            if (query != null && !query.isEmpty()) {
                accessToken = "";

                if (query.contains("code=")) {
                    accessToken = query.replace("code=", "");
                }
            }

            int errorCode;
            String message;

            if (accessToken.isEmpty()) {
                errorCode = 403;
                message = denied;
            } else {
                errorCode = 200;
                message = granted;
            }

            exchange.sendResponseHeaders(errorCode, message.length());
            exchange.getResponseBody().write(message.getBytes());
            exchange.getResponseBody().close();

            if (!accessToken.isEmpty()) {
                postTheCode();
            }
        });
    }

    static void auth() {

        String gotoUrl = spotifyAddress + "/authorize?client_id=" + client_id +
                "&redirect_uri=http://localhost:" + localPort + "&response_type=code";

        System.out.println("use this link to request the access code:");
        System.out.println(gotoUrl);
    }

    static void postTheCode() {
        String requestBody = String.format("grant_type=authorization_code&code=%s&redirect_uri=http://localhost:8080"
                + "&client_id=%s&client_secret=%s", accessToken, client_id, client_secret);

        HttpRequest request = HttpRequest.newBuilder()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create(spotifyAddress + "/api/token"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = null;

        while (response == null) {
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String body = response.body();

        if (body.contains("access_token")) {
            accessToken = JsonParser.parseString(body).getAsJsonObject().get("access_token").getAsString();

            System.out.println("---SUCCESS---");
            isLoggedIn = true;
        }
    }

    static void pause() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void saveCategories() {
        Properties properties = new Properties();

        for (Map.Entry<String,String> entry : categories.entrySet()) {
            properties.put(entry.getKey(), entry.getValue());
        }

        try {
            properties.store(new FileOutputStream("categories.dat"), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void loadCategories() {
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream("categories.dat"));
        } catch (IOException e) {
            System.out.println(); // "File has not created yet.";
        }

        for (String key : properties.stringPropertyNames()) {
            categories.put(key, properties.get(key).toString());
        }
    }
}
