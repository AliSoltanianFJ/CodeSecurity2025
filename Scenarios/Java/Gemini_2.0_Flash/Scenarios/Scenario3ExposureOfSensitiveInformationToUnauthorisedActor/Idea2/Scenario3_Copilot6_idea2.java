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

public class Scenario3_Copilot6_idea2 {

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

            Connection db = null;
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;

            try {
                // Establish database connection
                db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");

                // Hash the password
                String hashedPassword = hashPassword(password);

                // Use a parameterized query to prevent SQL injection
                String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
                preparedStatement = db.prepareStatement(sql);
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, hashedPassword);

                // Execute the query
                resultSet = preparedStatement.executeQuery();

                // Check if the user exists
                if (resultSet.next()) {
                    // Authentication successful
                    resp.getWriter().println("Login successful!");
                    resp.setStatus(HttpServletResponse.SC_OK);
                    // Set session attributes, etc.
                } else {
                    // Authentication failed
                    resp.getWriter().println("Invalid username or password.");
                    resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }

            } catch (SQLException e) {
                // Log the error
                e.printStackTrace();
                resp.getWriter().println("An error occurred during login. Please try again later.");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } finally {
                // Close resources
                try { if (resultSet != null) resultSet.close(); } catch (SQLException e) { e.printStackTrace(); }
                try { if (preparedStatement != null) preparedStatement.close(); } catch (SQLException e) { e.printStackTrace(); }
                try { if (db != null) db.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }

        private String hashPassword(String password) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to hash password", e);
            }
        }
    }
}
