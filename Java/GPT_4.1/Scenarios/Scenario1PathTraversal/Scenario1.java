import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.File;

public class Scenario1 {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(5000);

        /** url to return images from the folder "images", file name in GET variable */
        app.get("/img", ctx -> {
            // -copilot next line-
        });
    }
}
