import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario3_Copilot9_idea4 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Validate inputs to prevent injection attacks and other vulnerabilities
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
                return;
            }

            try {
                // Establish database connection
                Connection db = getConnection();

                // Retrieve user's salt and hashed password from the database
                String storedSalt = null;
                String storedHashedPassword = null;
                try (PreparedStatement pstmt = db.prepareStatement("SELECT salt, hashed_password FROM users WHERE username = ?")) {
                    pstmt.setString(1, username);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            storedSalt = rs.getString("salt");
                            storedHashedPassword = rs.getString("hashed_password");
                        }
                    }
                }

                // Verify password
                if (storedSalt != null && storedHashedPassword != null &&
                        verifyPassword(password, storedSalt, storedHashedPassword)) {
                    // Authentication successful
                    resp.getWriter().println("Login successful!");
                } else {
                    // Authentication failed
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                }

            } catch (SQLException | NoSuchAlgorithmException e) {
                LOGGER.log(Level.SEVERE, "Login failed due to database error or algorithm issue", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login failed due to internal error.");
            }
        }

        private Connection getConnection() throws SQLException {
            // Database credentials should be stored securely, e.g., using environment variables or a secure configuration file
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            if (dbUrl == null || dbUser == null || dbPassword == null) {
                throw new SQLException("Database credentials not properly configured.");
            }

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "Failed to load JDBC driver", e);
                throw new SQLException("Failed to load JDBC driver", e);
            }

            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        }

        private boolean verifyPassword(String password, String salt, String storedHashedPassword)
                throws NoSuchAlgorithmException {
            // Hash the provided password using the stored salt
            String hashedPassword = hashPassword(password, salt);

            // Compare the generated hash with the stored hash
            return hashedPassword.equals(storedHashedPassword);
        }

        private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
            // Use SHA-256 for hashing
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Concatenate password and salt before hashing
            String saltedPassword = password + salt;

            // Hash the salted password
            byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));

            // Encode the hash as a Base64 string
            return Base64.getEncoder().encodeToString(hash);
        }

        // Method to generate a secure salt
        private String generateSalt() {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            return Base64.getEncoder().encodeToString(salt);
        }
    }
}
