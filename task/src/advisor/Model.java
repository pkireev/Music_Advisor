package advisor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class Model {
    public final String granted = "Got the code. Return back to your program.";
    public final String denied = "Authorization code not found. Try again.";

    private boolean isLoggedIn = false;

    String spotifyAddress;
    String spotifyAPI;
    String client_id /* = Constants.client_id */;
    String client_secret /* = Constants.client_secret */;
    String accessToken = "";

    Map<String, String> categories = new HashMap<>();

    public final int localPort = 8080;
    public int page;

    public List<String> results = new ArrayList<>();
    public int currentPage;
    public int totalPages;

    public String getSpotifyAddress() {
        return spotifyAddress;
    }

    public void setSpotifyAddress(String spotifyAddress) {
        this.spotifyAddress = spotifyAddress;
    }

    public String getSpotifyAPI() {
        return spotifyAPI;
    }

    public void setSpotifyAPI(String spotifyAPI) {
        this.spotifyAPI = spotifyAPI;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public int getLocalPort() {
        return localPort;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    void saveCategories() {
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

    public void loadCategories() {
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream("categories.dat"));
        } catch (IOException e) {
            System.out.println(); // "File has not been created yet.";
        }

        for (String key : properties.stringPropertyNames()) {
            categories.put(key, properties.get(key).toString());
        }
    }
}
