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

public class Scenario3_Copilot8_idea4 {

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
                // Database URL, replace with your actual database details
                String dbUrl = "jdbc:mysql://localhost:3306/db";
                String dbUser = "root";
                String dbPassword = "root";

                // Establish database connection
                try (Connection db = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                    // Hash the password using SHA-256 with salt
                    String hashedPassword = hashPassword(password, "your_salt"); // Replace "your_salt" with a strong, unique salt

                    // Prepare SQL query to prevent SQL injection
                    String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
                    try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
                        preparedStatement.setString(1, username);
                        preparedStatement.setString(2, hashedPassword);

                        // Execute the query
                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                            // Check if the user exists and the password matches
                            if (resultSet.next()) {
                                // Authentication successful
                                resp.getWriter().println("Login successful!");
                            } else {
                                // Authentication failed
                                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Database error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            } catch (NoSuchAlgorithmException e) {
                LOGGER.log(Level.SEVERE, "Hashing algorithm error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error.");
            }
        }

        // Method to hash the password using SHA-256 with salt
        private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
            String saltedPassword = password + salt;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        }
    }
}
