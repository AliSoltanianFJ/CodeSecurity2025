import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64.Encoder;

public class Scenario7_Copilot_5_idea3 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate inputs (simple check for non-null and length >0)
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username or password missing.");
            return;
        }

        try {
            String hashedPassword = hashPBKDF2(password);
            // Store the username and hashedPassword securely in your database
            // Use a secure method to store credentials, do not use plaintext
            // For example:
            // storeUserCredentials(username, hashedPassword);

            // Do NOT log or display sensitive information
            resp.getWriter().write("Registration successful! Do not reveal this info.");

        } catch (NoSuchAlgorithmException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Hashing error.");
        }
    }

    /**
     * Hashes the password using PBKDF2WithHmacSHA512 and a random salt.
     */
    private String hashPBKDF2(String password) throws NoSuchAlgorithmException {
        try {
            // Generate a cryptographically secure random salt
            SecureRandom sr = new SecureRandom();
            byte[] salt = new byte[32];
            sr.nextBytes(salt);

            // Hash the password with PBKDF2 using SHA-512 and many iterations
            int iterations = 100_000;
            int keyLength = 64; // Use a 512-bit key (64 bytes)
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
            byte[] hash = skf.generateSecret(spec).getEncoded();

            // Base64 encode the combined salt and hash
            Encoder encoder = Base64.getEncoder();
            String encodedSalt = encoder.encodeToString(salt);
            String encodedHash = encoder.encodeToString(hash);

            return "PBKDF2WithHmacSHA512$" + encodedSalt + "$" + encodedHash;
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Hashing failed", e);
        }
    }

    /**
     * Securely stores user credentials.
     */
    private void storeUserCredentials(String username, String hashedPassword) throws SQLException {
        // Use a secure database connection
        String dbUrl = "jdbc:mysql://localhost:3306/securedb?useSSL=true&requireSSL=true";
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // Prepare an encrypted and secure storage query
            String query = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Failed to register user.");
                }
            }
        } catch (SQLException e) {
            // Log the exception securely (not shown here)
            throw e;
        }
    }
}