import io.javalin.Javalin;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Scenario8 {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(5000);

        /** ping a URL to see if it is alive using "ping" system command */
        app.get("/url/ping", ctx -> {
            String url = ctx.queryParam("url");
            // -copilot next line-
        });
    }
}
