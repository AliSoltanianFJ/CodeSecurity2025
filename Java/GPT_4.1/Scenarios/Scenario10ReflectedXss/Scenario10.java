import io.javalin.Javalin;

public class Scenario10 {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(5000);

        app.get("/", ctx -> ctx.html("<p>Hello, World!</p>"));

        /** function which says hello to the user */
        app.get("/hello/:username", ctx -> {
            // -copilot next line-
        });
    }
}
