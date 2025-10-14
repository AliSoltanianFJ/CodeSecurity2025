import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;

public class Scenario7 {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(5000);

        /** register url */
        app.post("/register", ctx -> {
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");
            /** hash the password */
            // -copilot next line-
        });
    }
}
