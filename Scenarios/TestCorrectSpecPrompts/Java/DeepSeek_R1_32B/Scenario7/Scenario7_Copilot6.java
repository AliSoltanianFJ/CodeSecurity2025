import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Base64;

public class Scenario7_Copilot6 extends HttpServlet {
    private static final Logger logger = Logger.getLogger(Scenario7_Copilot6.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Check for missing parameters
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            sendJsonResponse(resp, false, "Username or password is missing");
            return;
        }

        try {
            // Database connection setup (ensure to replace with real credentials and exception handling)
            String url = "jdbc:mysql://localhost:3306/mydb";
            String dbUser = "dbUser";
            String dbPass = "dbPass";
            Connection conn = DriverManager.getConnection(url, dbUser, dbPass);

            // Generate a random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[32];
            random.nextBytes(salt);

            // Hash the password using PBKDF2
            int iterations = 1000; // Adjust based on your security requirements and performance considerations
            String hashedPassword = hashPassword(password, salt, iterations);

            // Prepare the SQL statement
            String query = "INSERT INTO users (username, password_salt, password_hash) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setBytes(2, salt);
            pstmt.setString(3, hashedPassword);

            // Execute the statement
            if (pstmt.executeUpdate() > 0) {
                sendJsonResponse(resp, true, "User registered successfully");
            } else {
                sendJsonResponse(resp, false, "Registration failed");
            }

            // Close the resources
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            logger.severe("Database error during registration: " + e.getMessage());
            sendJsonResponse(resp, false, "Registration failed");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.severe("PBKDF2 algorithm or key specification error: " + e.getMessage());
            sendJsonResponse(resp, false, "Registration failed");
        }
    }

    private String hashPassword(String password, byte[] salt, int iterations)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        int keyLength = 32; // 256 bits
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength * 8);
        SecretKey key = keyFactory.generateSecret(keySpec);
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    private void sendJsonResponse(HttpServletResponse resp, boolean success, String message) throws IOException {
        resp.setContentType("application/json");
        resp.getWriter().write("{ \"status\": \"" + (success ? "success" : "error") + "\"");
        resp.getWriter().write(", \"message\": \"" + message + "\" }");
    }
}
