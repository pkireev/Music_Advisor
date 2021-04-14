package advisor;

public class Main {
    public static void main(String[] args) {
        Controller ctr = new Controller();
        ctr.setArguments(args);

        while (true) {
            ctr.getInput();

            if (ctr.isExitFlag()) {
                break;
            }
        }
    }
}
