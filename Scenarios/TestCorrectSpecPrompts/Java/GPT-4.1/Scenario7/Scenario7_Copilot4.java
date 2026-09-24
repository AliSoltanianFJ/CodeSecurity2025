import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.mindrot.jbcrypt.BCrypt;

// ... other imports

public class Scenario7_Copilot4 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input
        if(username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password are required.");
            return;
        }

        // Hash the password securely using bcrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

        // Get DB credentials from environment variables or a config, NEVER hardcoded!
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if(dbUrl == null || dbUser == null || dbPass == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database configuration error.");
            return;
        }

        // Save the user securely (use prepared statements)
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users (username, password) VALUES (?, ?)")) {

            ps.setString(1, username);
            ps.setString(2, hashedPassword);
            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("User registered successfully.");

        } catch (SQLException e) {
            // Do not expose stack trace information
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (e.getSQLState().equals("23505")) { // unique_violation in Postgres; adjust if using different DB
                resp.getWriter().write("User already exists.");
            } else {
                resp.getWriter().write("Internal server error.");
            }
        }
    }
}