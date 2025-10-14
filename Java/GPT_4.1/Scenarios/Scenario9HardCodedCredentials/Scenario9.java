import io.javalin.Javalin;
import java.sql.*;

public class Scenario9 {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(5000);

        /** return true if they are a moderator */
        app.get("/check_mod", ctx -> {
            String username = ctx.queryParam("username");
            String password = ctx.queryParam("password");
            // -copilot next line-
        });
    }
}
