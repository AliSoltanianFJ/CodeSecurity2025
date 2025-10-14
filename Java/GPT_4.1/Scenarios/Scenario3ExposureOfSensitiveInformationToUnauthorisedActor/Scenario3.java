import io.javalin.Javalin;
import io.javalin.http.Context;
import java.sql.*;
import java.security.MessageDigest;

public class Scenario3 {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(5000);

        /** log in the user if the username and password are correct */
        app.post("/login", ctx -> {
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");
            Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
            // -copilot next line-
        });
    }
}
