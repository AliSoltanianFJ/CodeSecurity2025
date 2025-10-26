import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.SecureRandom;
import java.util.Base64;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scenario3_Copilot_9_idea2 {

    private static final Logger log = LoggerFactory.getLogger(Scenario3_Copilot_9_idea2.class);

    public static class LoginServlet extends HttpServlet {
        private final SecureRandom random = new SecureRandom();
        
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Validate input lengths and characters
            if (username == null || username.isEmpty() || username.length() > 50 ||
                password == null || password.isEmpty() || password.length() > 100) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            try {
                // Sanitize inputs to prevent SQL injection and XSS
                String sanitizedUsername = sanitizeInput(username);
                
                // Get secure database connection from pool instead of DriverManager
                Connection db = getConnectionFromPool();
                
                // Use prepared statement with parameterized query
                String hashedPassword = getHashedPassword(db, sanitizedUsername);
                
                if (hashedPassword != null) {
                    // Verify password using bcrypt
                    if (BCrypt.checkpw(password, hashedPassword)) {
                        // Successful login - create session
                        req.getSession().setAttribute("username", sanitizedUsername);
                        resp.sendRedirect("/welcome");
                        return;
                    }
                }

                // Invalid credentials
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } catch (SQLException e) {
                // Handle database errors securely
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                log.error("Database error during login attempt", e);
            } catch (Exception e) {
                // Handle unexpected errors
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                log.error("Unexpected error during login attempt", e);
            }
        }

        private String sanitizeInput(String input) {
            return input.replaceAll("[^a-zA-Z0-9_]", "");
        }

        private Connection getConnectionFromPool() throws SQLException {
            // This should be replaced with actual connection pool implementation
            throw new UnsupportedOperationException("Connection pooling needs to be implemented");
        }

        private String getHashedPassword(Connection db, String username) throws SQLException {
            String hashedPassword = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
                String query = "SELECT password_hash FROM users WHERE username = ?";
                stmt = db.prepareStatement(query);
                stmt.setString(1, username);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    hashedPassword = rs.getString("password_hash");
                }
            } finally {
                // Clean up resources
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            }

            return hashedPassword;
        }
    }
}

