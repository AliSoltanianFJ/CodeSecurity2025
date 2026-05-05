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

public class Scenario3_Copilot6_idea4 {

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

                // Hash the password using SHA-256 with salt
                String hashedPassword = hashPassword(password, "your_salt"); // Replace "your_salt" with a strong, unique salt

                // Use prepared statements to prevent SQL injection
                String query = "SELECT * FROM users WHERE username = ? AND password = ?";
                PreparedStatement pstmt = db.prepareStatement(query);
                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);

                ResultSet rs = pstmt.executeQuery();

                // Check if the user exists and the password is correct
                if (rs.next()) {
                    // Authentication successful
                    resp.getWriter().println("Login successful!");
                } else {
                    // Authentication failed
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

        // Hash the password using SHA-256 with salt
        private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
            String saltedPassword = password + salt;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        }
    }
}
