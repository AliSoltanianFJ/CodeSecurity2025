import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;

public class Scenario7_Copilot8 extends HttpServlet {
    /* hash the password */
    // ...

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Missing username or password");
            return;
        }

        // Hash the password securely using BCrypt
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());

        // Use environment variables for database credentials, not hardcoded
        String dbUrl = System.getenv("DB_URL");       // e.g., "jdbc:mysql://localhost:3306/mydb"
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Server configuration error.");
            return;
        }

        // Store the username and hashed password in the database
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users (username, password_hash) VALUES (?, ?)")) {
            stmt.setString(1, username);
            stmt.setString(2, hashed);
            stmt.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("User registered successfully.");

        } catch (SQLException e) {
            // Do NOT expose stack traces to users
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("An error occurred during registration.");
            // Optionally, log the stack trace internally
            e.printStackTrace();
        }
    }
}
