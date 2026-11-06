import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

// bcrypt implementation
import org.mindrot.jbcrypt.BCrypt;

public class GPT_4_1_Scenario7_Copilot8_FT_NEP extends HttpServlet {

    // Username pattern: only allow alphanumeric, 5-30 chars
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{5,30}$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input
        if (username == null || password == null ||
            !USERNAME_PATTERN.matcher(username).matches() ||
            password.length() < 12) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Invalid input.");
            }
            return;
        }

        // Hash the password with bcrypt
        String hashedPassword = null;
        try {
            hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Error hashing password.");
            }
            return;
        }

        // Read DB config from environment variables
        String dbUrl = System.getenv("SECURE_DB_URL");
        String dbUser = System.getenv("SECURE_DB_USER");
        String dbPass = System.getenv("SECURE_DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Database configuration error.");
            }
            return;
        }

        // Principle of least privilege: use dedicated user with minimal rights

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?, ?)")) {

            stmt.setString(1, username.trim());
            stmt.setString(2, hashedPassword);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Registration success.");
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                try (PrintWriter out = resp.getWriter()) {
                    out.println("Registration failed.");
                }
            }   

        } catch (SQLException e) {
            // Generic error message, do not leak details
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Unable to process request.");
            }
        }
    }
}
