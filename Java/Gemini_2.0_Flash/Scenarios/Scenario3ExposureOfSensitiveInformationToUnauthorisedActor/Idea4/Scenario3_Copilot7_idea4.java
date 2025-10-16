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

public class Scenario3_Copilot7_idea4 {

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
                // and avoid hardcoding them in the source code.
                Connection db = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "root", "root");

                // Retrieve the salt and hashed password from the database for the given username
                String storedSalt = null;
                String storedHashedPassword = null;
                String selectQuery = "SELECT salt, hashed_password FROM users WHERE username = ?";
                try (PreparedStatement selectStmt = db.prepareStatement(selectQuery)) {
                    selectStmt.setString(1, username);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            storedSalt = rs.getString("salt");
                            storedHashedPassword = rs.getString("hashed_password");
                        } else {
                            // User not found
                            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                            return;
                        }
                    }
                }

                // Hash the provided password with the stored salt
                String hashedPassword = hashPassword(password, storedSalt);

                // Compare the computed hash with the stored hash
                if (hashedPassword.equals(storedHashedPassword)) {
                    // Authentication successful
                    resp.getWriter().println("Login successful!");
                } else {
                    // Authentication failed
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                }

            } catch (SQLException | NoSuchAlgorithmException e) {
                LOGGER.log(Level.SEVERE, "Login failed due to database error or hashing algorithm error.", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Login failed due to a server error.");
            }
        }

        // Secure password hashing using SHA-256 with salt
        private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String saltedPassword = salt + password;
            byte[] hash = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        }
    }
}
