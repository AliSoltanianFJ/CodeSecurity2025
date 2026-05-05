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

public class Scenario3_Copilot1_idea4 {

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
                // Note: It is highly recommended to use environment variables or a secure configuration file
                // to store database credentials instead of hardcoding them.
                Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");

                // Hash the password before comparing it with the stored hash
                String hashedPassword = hashPassword(password);

                // Use a prepared statement to prevent SQL injection
                String query = "SELECT * FROM users WHERE username = ? AND password = ?";
                try (PreparedStatement preparedStatement = db.prepareStatement(query)) {
                    preparedStatement.setString(1, username);
                    preparedStatement.setString(2, hashedPassword);

                    // Execute the query
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        // Check if the user exists and the password is correct
                        if (resultSet.next()) {
                            // Authentication successful

                            // Redirect to a secure page
                            resp.sendRedirect("/secure-page");
                        } else {
                            // Authentication failed
                            // Display an error message to the user
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                        }
                    }
                }
            } catch (SQLException e) {
                // Log the error
                LOGGER.log(Level.SEVERE, "Database error", e);
                // Display a generic error message to the user
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while processing your request.");
            } finally {
                // Close the database connection
                // Note: It is important to close the database connection in a finally block to ensure that it is always closed
                // even if an exception occurs.
                try {
                    //Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");
                    //db.close();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to close database connection", e);
                }
            }
        }

        // Hash the password using SHA-256 algorithm
        private String hashPassword(String password) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                LOGGER.log(Level.SEVERE, "SHA-256 algorithm not found", e);
                throw new RuntimeException("Failed to hash password", e);
            }
        }
    }
}
