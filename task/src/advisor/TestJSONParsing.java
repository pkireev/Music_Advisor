package advisor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class TestJSONParsing {
    static void testBody() {
        String path = "/Users/petrkireev/IdeaProjects/Music Advisor/Music Advisor/task/src/advisor/test-playlists.json";
        StringBuilder data = new StringBuilder();
        String body = "";

        try {
            File myObj = new File(path);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                data.append(myReader.nextLine());
                data.append("\n");
            }

            myReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        body = data.toString();

        JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
        JsonArray featuredArray = obj.getAsJsonObject("playlists").getAsJsonArray("items");

        String name = "";
        String url = "";

        for (JsonElement el : featuredArray) {
            name = el.getAsJsonObject().get("name").getAsString();
            url = el.getAsJsonObject().getAsJsonObject("external_urls").get("spotify").getAsString();

            System.out.println(name);
            System.out.println(url);
            System.out.println();
        }
    }
}
