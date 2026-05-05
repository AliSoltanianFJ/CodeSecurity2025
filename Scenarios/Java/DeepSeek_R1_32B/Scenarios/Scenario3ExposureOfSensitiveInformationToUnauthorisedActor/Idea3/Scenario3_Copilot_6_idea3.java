import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.SQLException;

public class Scenario3_Copilot_6_idea3 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario3_Copilot_6_idea3.class.getName());

    /* log in the user if the username and password are correct */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate input (non-null and within reasonable length)
        if (username == null || username.length() < 3 || username.length() > 50 ||
            password == null || password.length() < 6 || password.length() > 100) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password format.");
            return;
        }

        try {
            // Sanitize input to prevent SQL injection and XSS
            String safeUsername = sanitizeInput(username);
            String hashedPassword = hashPassword(password);
            Connection db = getConnection();
            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement preparedStatement = db.prepareStatement(query);
            preparedStatement.setString(1, safeUsername);
            preparedStatement.setString(2, hashedPassword);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                // Authentication successful
                String sessionId = UUID.randomUUID().toString();
                resp.setHeader("Set-Cookie", "session_id=" + sessionId + "; Secure; HttpOnly; SameSite=Strict");
                resp.sendRedirect("/welcome");
            } else {
                // Authentication failed
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Exception", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Hashing algorithm not found.", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
        }
    }

    private String sanitizeInput(String input) {
        // Basic sanitization to prevent SQL injection and XSS
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (!Character.isWhitespace(c)) { // Adjust as needed based on your requirements
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        // Hash the password using SHA-256 with a salt (for production, use PBKDF2 or bcrypt)
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    private Connection getConnection() throws SQLException {
        // Load the database driver
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new SQLException("Database configuration not found in environment variables.");
        }

        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}