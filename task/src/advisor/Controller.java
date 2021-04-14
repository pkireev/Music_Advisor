package advisor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Controller {
    public Scanner sc;
    public View view;
    public Model model;

    public HttpServer server = null;
    public HttpClient client = null;

    private boolean exitFlag = false;

    public Controller() {
        sc = new Scanner(System.in);
        view = new View();
        model = new Model();

        // DEFAULT VALUES
        model.setSpotifyAddress("https://accounts.spotify.com");
        model.setSpotifyAPI("https://api.spotify.com");
        model.setPage(5);

        model.loadCategories();
    }

    public void setArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("-access".equals(args[i])) {
                model.setSpotifyAddress(args[i + 1]);
            }

            if ("-resource".equals(args[i])) {
                model.setSpotifyAPI(args[i + 1]);
            }

            if ("-page".equals(args[i])) {
                model.setPage(Integer.parseInt(args[i + 1]));
            }
        }
    }

    public void getInput() {
        pause();
        pause();

        String input = sc.nextLine();
        String plist = "";

        if (input.contains("playlists")) {
            plist = input.replace("playlists ", "");
            input = "playlists";
        }

        switch (input) {
            case "auth":
                pause();
                getServer();
                createContext();

                auth();
                view.print("waiting for code...");
                break;

            case "featured":
                getFeatured();
                sendToView();
                break;

            case "new":
                getNew();
                sendToView();
                break;

            case "categories":
                getCategories();
                sendToView();
                break;

            case "playlists":
                getPlaylists(plist);
                sendToView();
                break;

            case "next":
                seeNext();
                break;

            case "prev":
                seePrev();
                break;

            case "exit":
                pause();
                view.print("---GOODBYE!---");
                break;
        }

        if ("exit".equals(input)) {
            exitFlag = true;
        }
    }

    public boolean isExitFlag() {
        return exitFlag;
    }

    private void pause() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void createContext() {
        getServer().createContext("/", exchange -> {
            String query = exchange.getRequestURI().getQuery();

            if (query != null && !query.isEmpty()) {
                model.setAccessToken("");

                if (query.contains("code=")) {
                    model.setAccessToken(query.replace("code=", ""));
                }
            }

            int errorCode;
            String message;

            if (model.getAccessToken().isEmpty()) {
                errorCode = 403;
                message = model.denied;
            } else {
                errorCode = 200;
                message = model.granted;
            }

            exchange.sendResponseHeaders(errorCode, message.length());
            exchange.getResponseBody().write(message.getBytes());
            exchange.getResponseBody().close();

            if (!model.getAccessToken().isEmpty()) {
                postTheCode();
            }
        });
    }

    public void postTheCode() {
        String requestBody = String.format("grant_type=authorization_code&code=%s&redirect_uri=http://localhost:8080"
                + "&client_id=%s&client_secret=%s", model.getAccessToken(), model.client_id, model.client_secret);

        HttpRequest request = HttpRequest.newBuilder()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create(model.getSpotifyAddress() + "/api/token"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = null;

        while (response == null) {
            try {
                response = getClient().send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String body = response.body();

        if (body.contains("access_token")) {
            view.print("---SUCCESS---");

            model.setAccessToken(JsonParser.parseString(body).getAsJsonObject().get("access_token").getAsString());
            model.setLoggedIn(true);

            server.stop(0);
            server = null;
        }
    }

    public void auth() {
        String gotoUrl = model.getSpotifyAddress() + "/authorize?client_id=" + model.client_id +
                "&redirect_uri=http://localhost:" + model.localPort + "&response_type=code";

        view.print("use this link to request the access code:");
        view.print(gotoUrl);
    }

    public void getFeatured() {
        pause();

        if (model.isLoggedIn()) {
            String apiPath = model.getSpotifyAPI() + "/v1/browse/featured-playlists";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + model.getAccessToken())
                    .uri(URI.create(apiPath))
                    .GET()
                    .build();

            HttpResponse<String> response = null;

            try {
                response = getClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (response != null) {
                JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray featuredArray = obj.getAsJsonObject("playlists").getAsJsonArray("items");

                String name;
                String url;

                model.results.clear();
                model.currentPage = 1;

                for (JsonElement el : featuredArray) {
                    name = el.getAsJsonObject().get("name").getAsString();
                    url = el.getAsJsonObject().getAsJsonObject("external_urls").get("spotify").getAsString();

                    model.results.add(name + "\n" + url + "\n");
                }

                setPages();
            }
        } else {
            view.print("Please, provide access for application.");
        }
    }

    public void getNew() {
        pause();

        if (model.isLoggedIn()) {
            String apiPath = model.getSpotifyAPI() + "/v1/browse/new-releases";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + model.getAccessToken())
                    .uri(URI.create(apiPath))
                    .GET()
                    .build();

            HttpResponse<String> response = null;

            try {
                response = getClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (response != null) {
                JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray items = obj.getAsJsonObject("albums").getAsJsonArray("items");

                String name;
                String url;

                model.results.clear();

                for (JsonElement el : items) {
                    List<String> artists = new ArrayList<>();

                    JsonArray artistsArray = el.getAsJsonObject().getAsJsonArray("artists");
                    for (JsonElement artistEl : artistsArray) {
                        artists.add(artistEl.getAsJsonObject().get("name").getAsString());
                    }

                    name = el.getAsJsonObject().get("name").getAsString();
                    url = el.getAsJsonObject().getAsJsonObject("external_urls").get("spotify").getAsString();

                    model.results.add(name + "\n" + artists + "\n" + url + "\n");
                }

                setPages();
            }
        } else {
            view.print("Please, provide access for application.");
        }
    }

    public void getCategories() {
        pause();

        if (model.isLoggedIn()) {
            String apiPath = model.getSpotifyAPI() + "/v1/browse/categories";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + model.getAccessToken())
                    .uri(URI.create(apiPath))
                    .GET()
                    .build();

            HttpResponse<String> response = null;

            try {
                response = getClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (response != null) {
                JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray featuredArray = obj.getAsJsonObject("categories").getAsJsonArray("items");

                String name;
                String id;

                model.results.clear();

                for (JsonElement el : featuredArray) {
                    name = el.getAsJsonObject().get("name").getAsString();
                    id = el.getAsJsonObject().get("id").getAsString();

                    model.results.add(name);
                    model.categories.put(name, id);
                }

                model.saveCategories();
                setPages();
            }
        } else {
            view.print("Please, provide access for application.");
        }
    }

    public void getPlaylists(String plist) {
        pause();

        if (model.isLoggedIn()) {
            String apiPath = String.format(model.getSpotifyAPI() + "/v1/browse/categories/%s/playlists", model.categories.get(plist));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + model.getAccessToken())
                    .uri(URI.create(apiPath))
                    .GET()
                    .build();

            HttpResponse<String> response = null;

            try {
                response = getClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (response != null) {
                if (response.body().contains("error")) {
                    view.print("Test unpredictable error message"); //"Specified id doesn't exist";
                }

                if (response.statusCode() == 404) {
                    view.print("Specified id doesn't exist"); //"Unknown category name.";
                }

                JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray featuredArray = obj.getAsJsonObject("playlists").getAsJsonArray("items");

                String name;
                String url;

                model.results.clear();

                for (JsonElement el : featuredArray) {
                    name = el.getAsJsonObject().get("name").getAsString();
                    url = el.getAsJsonObject().getAsJsonObject("external_urls").get("spotify").getAsString();

                    model.results.add(name + "\n" + url + "\n");
                }

                setPages();
            }
        } else {
            view.print("Please, provide access for application.");
        }
    }

    public void sendToView() {
        for (int i = (model.currentPage - 1) * model.getPage(); i < model.currentPage * model.getPage(); i++) {
            if (i < model.results.size()) {
                view.print(model.results.get(i));
            }
        }
        view.print(String.format("---PAGE %d OF %d---", model.currentPage, model.totalPages));
    }

    public void seeNext() {
        if (model.currentPage + 1 > model.totalPages) {
            view.print("No more pages.");
        } else {
            model.currentPage++;
            sendToView();
        }
    }

    public void seePrev() {
        if (model.currentPage == 1) {
            view.print("No more pages.");
        } else {
            model.currentPage--;
            sendToView();
        }
    }

    private void setPages() {
        model.currentPage = 1;
        model.totalPages = model.results.size() / model.page;
        if (model.results.size() % model.page != 0) {
            model.totalPages++;
        }
    }

    public HttpServer getServer() {
        if (server == null) {
            try {
                server =HttpServer.create(new InetSocketAddress(model.getLocalPort()), 0);
                server.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return server;
    }

    public HttpClient getClient() {
        if (client == null) {
            return HttpClient.newBuilder().build();
        }
        return client;
    }
}
