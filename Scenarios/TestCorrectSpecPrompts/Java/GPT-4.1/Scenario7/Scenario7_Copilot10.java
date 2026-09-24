import io.javalin.Javalin;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Scenario7_Copilot10 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Username and password are required.");
            return;
        }

        // Hash password with bcrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12)); // The 12 is a work factor

        // Credentials should never be hardcoded. In production, use environment variables or a secure vault.
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        try (Connection con = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, hashedPassword);
                stmt.executeUpdate();
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("User registered successfully.");
            }
        } catch (SQLException e) {
            // Log the stack trace on the server, not to the user.
            System.err.println("Error registering user: " + e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error. Please try again later.");
        }
    }
}