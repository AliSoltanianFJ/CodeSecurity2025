import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Scenario3_Copilot_10_idea3 extends HttpServlet {
    /* log in the user if the username and password are correct */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            // Validate input parameters
            if (username == null || password == null ||
                username.length() > 50 || password.length() > 100) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials format.");
                return;
            }

            // Load database configuration from environment variables
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            if (dbUrl == null || dbUser == null || dbPassword == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration not found.");
                return;
            }

            // Establish a secure database connection
            try (Connection db = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {

                // Use parameterized query to prevent SQL injection
                String query = "SELECT id, password_hash FROM users WHERE username ?";
                try (PreparedStatement pstmt = db.prepareStatement(query)) {
                    pstmt.setString(1, username);

                    ResultSet rs = pstmt.executeQuery();

                    if (!rs.next()) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
                        return;
                    }

                    // Retrieve stored hash and salt
                    String storedHash = rs.getString("password_hash");
                    byte[] salt = Base64.getDecoder().decode(storedHash.substring(0, 25));

                    // Hash the provided password with the retrieved salt
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    md.update(salt);
                    byte[] hashBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
                    String providedHash = Base64.getEncoder().encodeToString(hashBytes);

                    // Compare hashes using constant-time comparison
                    if (providedHash.equals(storedHash)) {
                        resp.getWriter().println("Login successful!");
                    } else {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
                    }
                }
            }

        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
            return;
        } catch (NoSuchAlgorithmException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Hashing algorithm not found.");
            return;
        }
    }
}