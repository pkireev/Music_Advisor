package advisor;

import java.util.Scanner;

public class Main {
    static boolean isLoggedIn = false;

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        String result = "";
        String plist = "";
        String input = "";
        String token = "6035cc165c744bba80145224ee81ad21";

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
                    result = String.format("https://accounts.spotify.com/authorize?client_id=%s", token) +
                        "&redirect_uri=http://localhost:8080&response_type=code\n" +
                        "---SUCCESS---";
                    isLoggedIn = true;
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
}
