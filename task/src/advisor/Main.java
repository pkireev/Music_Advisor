package advisor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class Main {
    static final String granted = "Got the code. Return back to your program.";
    static final String denied = "Authorization code not found. Try again.";
    static boolean isLoggedIn = false;
    static HttpServer server;
    static HttpClient client = HttpClient.newBuilder().build();
    static String spotifyAddress = "https://accounts.spotify.com";
    static String client_id = "6035cc165c744bba80145224ee81ad21";
    static String code = "";

    public static void main(String[] args) {

        if (args.length >= 2) {
            if ("-access".equals(args[0])) {
                spotifyAddress = args[1];
            }
        }

        Scanner sc = new Scanner(System.in);
        String result = "";
        String plist = "";
        String input = "";


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
                    createServer();
                    createContext();
                    server.start();

                    auth();
                    result = "waiting for code...";
                    break;

                default:
                    result = "";
            }

            System.out.println(result);

            if ("exit".equals(input)) {
                break;
            }
        }
    }

    static String getFeatured() {
        if (isLoggedIn) {
            return "---FEATURED---\n" +
                    "Mellow Morning\n" +
                    "Wake Up and Smell the Coffee\n" +
                    "Monday Motivation\n" +
                    "Songs to Sing in the Shower";
        } else {
            return "Please, provide access for application.";
        }
    }

    static String getNew() {
        if (isLoggedIn) {
            return "---NEW RELEASES---\n" +
                    "Mountains [Sia, Diplo, Labrinth]\n" +
                    "Runaway [Lil Peep]\n" +
                    "The Greatest Show [Panic! At The Disco]\n" +
                    "All Out Life [Slipknot]";
        } else {
            return "Please, provide access for application.";
        }
    }

    static String getCategories() {
        if (isLoggedIn) {
            return  "---CATEGORIES---\n"+
                    "Top Lists\n" +
                    "Pop\n" +
                    "Mood\n" +
                    "Latin";
        } else {
            return "Please, provide access for application.";
        }
    }

    static String getPlaylists(String plist) {
        if (isLoggedIn) {
            return "---" + plist.toUpperCase() + " PLAYLISTS---\n" +
                    "Walk Like A Badass  \n" +
                    "Rage Beats  \n" +
                    "Arab Mood Booster  \n" +
                    "Sunday Stroll";
        } else {
            return "Please, provide access for application.";
        }
    }


    static void createServer() {
        try {
            server = HttpServer.create();
            server.bind(new InetSocketAddress(8080), 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void createContext() {

        server.createContext("/", exchange -> {
            code = "";
            String query = exchange.getRequestURI().getQuery();

            if (query != null && !query.isEmpty()) {
                if (query.contains("code=")) {
                    code = query.replace("code=", "");
                }
            }

            int errorCode = 0;
            String message = "";

            if (code.isEmpty()) {
                errorCode = 403;
                message = denied;
            } else {
                errorCode = 200;
                message = granted;
            }

            exchange.sendResponseHeaders(errorCode, message.length());
            exchange.getResponseBody().write(message.getBytes());
            exchange.getResponseBody().close();

            if (!code.isEmpty()) {
                postTheCode();
                server.stop(1);
            }
        });
    }



//    static void makeRequest(String url) {
//
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(url))
//                .GET()
//                .build();
//
//        HttpResponse<String> response = null;
//
//        try {
//            response = client.send(request, HttpResponse.BodyHandlers.ofString());
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        if (response != null) {
//            System.out.println(response.body());
//        }
//    }


    static void auth() {

        String gotoUrl = String.format("%s/authorize?client_id=%s", spotifyAddress, client_id) +
                "&redirect_uri=http://localhost:8080&response_type=code";

        System.out.println("use this link to request the access code:");
        System.out.println(gotoUrl);

    }

    static void fakePostTheCode() {
        String b = "{" +
                "\"access_token\":\"456456\"," +
                "\"token_type\":\"Bearer\"," +
                "\"expires_in\":3600," +
                "\"refresh_token\":" + "\"567567\"," +
                "\"scope\":\"\"" +
                "}";

        System.out.println(b);
        isLoggedIn = true;
    }

    static void postTheCode() {

        String requestBody = String.format("grant_type=authorization_code&code=%s&redirect_uri=http://localhost:8080", code)
                + "&client_id=6035cc165c744bba80145224ee81ad21&client_secret=2040b45594d94f469649568c6b8fe48d";
        String authHeader = "Authorization: Basic NjAzNWNjMTY1Yzc0NGJiYTgwMTQ1MjI0ZWU4MWFkMjE6MjA0MGI0NTU5NGQ5NGY0Njk2NDk1NjhjNmI4ZmU0OGQ=";

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
        System.out.println(body);

        if (body.contains("access_token")) {
            System.out.println("---SUCCESS---");
            isLoggedIn = true;
        }
    }
}
