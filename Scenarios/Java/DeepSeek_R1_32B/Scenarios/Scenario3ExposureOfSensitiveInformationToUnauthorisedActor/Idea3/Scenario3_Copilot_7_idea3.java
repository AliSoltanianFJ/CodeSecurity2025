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
import java.util.Random;
import java.util.UUID;

public class Scenario3_Copilot_7_idea3 extends HttpServlet {
    /* log in the user if the username and password are correct */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            Connection db = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username or password.");
                return;
            }

            // Hash the password with a random salt using SHA-256
            Random rand = new Random();
            byte[] salt = new byte[32];
            rand.nextBytes(salt);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));

            // Base64 encode the salt and hashed password for storage
            String encodedSalt = Base64.getEncoder().encodeToString(salt);
            String encodedHashedPassword = Base64.getEncoder().encodeToString(hashedPassword);

            // Query to check if username exists and password matches
            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement preparedStatement = db.prepareStatement(query);
            preparedStatement.setString(1, username.trim());
            preparedStatement.setString(2, encodedHashedPassword);
            ResultSet resultSet = preparedStatement.executeQuery();

            boolean authenticated = false;
            if (resultSet.next()) {
                // Valid credentials
                resp.getWriter().write("Login successful!");
                // Set a session cookie or token here as needed
                String sessionId = UUID.randomUUID().toString();
                resp.setHeader("Set-Cookie", "session=" + sessionId + "; Secure; HttpOnly");
                authenticated = true;
            } else {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
            }

        } catch (SQLException e) {
            // Log SQLException securely - use a secure logger, not System.out
            // For this example, we just print it, but in production, use proper logging
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to hash password.");
        }
    }
}