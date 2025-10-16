import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario3_Copilot5_idea4 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Validate inputs to prevent injection attacks and null pointer exceptions
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
                return;
            }

            try {
                // Establish database connection
                // Note: It is highly recommended to use environment variables or a secure configuration file to store database credentials
                // to avoid hardcoding them in the source code.
                Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");

                // Use prepared statements to prevent SQL injection
                String sql = "SELECT password_hash, salt FROM users WHERE username = ?";
                PreparedStatement pstmt = db.prepareStatement(sql);
                pstmt.setString(1, username);

                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String salt = rs.getString("salt");

                    // Verify password
                    if (verifyPassword(password, storedHash, salt)) {
                        // Authentication successful
                        resp.getWriter().println("Login successful!");
                    } else {
                        // Authentication failed
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                    }
                } else {
                    // User not found
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                }

                // Close resources
                rs.close();
                pstmt.close();
                db.close();

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while processing your request.");
            } catch (NoSuchAlgorithmException e) {
                LOGGER.log(Level.SEVERE, "Hashing algorithm error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while processing your request.");
            }
        }

        /**
         * Verifies the provided password against the stored hash using the salt.
         * @param password The password to verify.
         * @param storedHash The stored password hash.
         * @param salt The salt used to hash the password.
         * @return True if the password is correct, false otherwise.
         * @throws NoSuchAlgorithmException If the hashing algorithm is not available.
         */
        private boolean verifyPassword(String password, String storedHash, String salt) throws NoSuchAlgorithmException {
            String hashedPassword = hashPassword(password, salt);
            return hashedPassword.equals(storedHash);
        }

        /**
         * Hashes the password using SHA-256 algorithm and the provided salt.
         * @param password The password to hash.
         * @param salt The salt to use for hashing.
         * @return The hashed password.
         * @throws NoSuchAlgorithmException If the SHA-256 algorithm is not available.
         */
        private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
            String saltedPassword = password + salt;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        }
    }
}
