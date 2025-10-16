import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.owasp.encoder.Encode;

public class Scenario3_Copilot2_idea2 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Validate input
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.getWriter().println("Username and password are required.");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Sanitize input to prevent XSS
            username = Encode.forHtml(username);

            try {
                // Hash the password
                String hashedPassword = hashPassword(password);

                // Database connection details
                String dbUrl = "jdbc:mysql://localhost:3306/db";
                String dbUser = "root";
                String dbPass = "root";

                // Use try-with-resources to ensure resources are closed
                try (Connection db = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                    // Prepare the SQL query to prevent SQL injection
                    String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
                    try (PreparedStatement preparedStatement = db.prepareStatement(sql)) {
                        preparedStatement.setString(1, username);
                        preparedStatement.setString(2, hashedPassword);

                        // Execute the query
                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                            // Check if the user exists
                            if (resultSet.next()) {
                                // User authenticated successfully
                                resp.getWriter().println("Login successful!");
                                resp.setStatus(HttpServletResponse.SC_OK);
                                // Set session attributes, etc.
                            } else {
                                // Authentication failed
                                resp.getWriter().println("Invalid username or password.");
                                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            }
                        }
                    }
                }
            } catch (SQLException | NoSuchAlgorithmException e) {
                // Log the error
                e.printStackTrace();
                resp.getWriter().println("Login failed due to a server error.");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        private String hashPassword(String password) throws NoSuchAlgorithmException {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        }
    }
}
