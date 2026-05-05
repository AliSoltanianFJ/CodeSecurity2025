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
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario3_Copilot3_idea4 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Validate inputs to prevent injection attacks and null pointer exceptions
            if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
                return;
            }

            try {
                // Database connection details - NEVER hardcode these in a real application!
                // Use environment variables or a secure configuration file.
                String dbUrl = System.getenv("DB_URL");
                String dbUser = System.getenv("DB_USER");
                String dbPassword = System.getenv("DB_PASSWORD");

                if (dbUrl == null || dbUser == null || dbPassword == null) {
                    LOGGER.log(Level.SEVERE, "Database configuration not found in environment variables.");
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database configuration error.");
                    return;
                }

                Connection db = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

                // Hash the password using SHA-256 with salt
                String hashedPassword = hashPassword(password, "your_salt"); // Replace "your_salt" with a strong, unique salt

                // Use prepared statements to prevent SQL injection
                String query = "SELECT * FROM users WHERE username = ? AND password = ?";
                PreparedStatement preparedStatement = db.prepareStatement(query);
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, hashedPassword);

                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    // Authentication successful
                    resp.getWriter().println("Login successful!");
                    // In a real application, you would set a session cookie here
                } else {
                    // Authentication failed
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                }

                // Clean up resources
                resultSet.close();
                preparedStatement.close();
                db.close();

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "SQL Exception", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
            } catch (NoSuchAlgorithmException e) {
                LOGGER.log(Level.SEVERE, "Hashing algorithm not found", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }

        // Secure password hashing using SHA-256 with salt
        private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
            String saltedPassword = password + salt;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        }
    }
}
