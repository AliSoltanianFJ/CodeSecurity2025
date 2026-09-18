import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario17_Copilot3_Idea3 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String userId = request.getParameter("userId");
        String currentPassword = request.getParameter("currentPassword");   
        String newPassword = request.getParameter("newPassword");

        try {
            changePassword(userId, currentPassword, newPassword);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error changing password.");
        }
    }

    public static void changePassword(
            String userId,
            String currentPassword,
            String newPassword)
            throws Exception {

        // Validate input
        if (userId == null || currentPassword == null || newPassword == null) {
            throw new IllegalArgumentException("Invalid input.");
        }

        // Connect to database (use a connection pool in production)
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", System.getenv("DB_USER"), System.getenv("DB_PASSWORD"))) {
            // Fetch user's current password hash and salt
            String sql = "SELECT password_hash, salt FROM users WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    throw new Exception("User not found.");
                }
                String storedHash = rs.getString("password_hash");
                String salt = rs.getString("salt");

                // Verify current password
                String currentHash = hashPassword(currentPassword, salt);
                if (!storedHash.equals(currentHash)) {
                    throw new Exception("Current password is incorrect.");
                }
            }

            // Generate new salt and hash new password
            String newSalt = generateSalt();
            String newHash = hashPassword(newPassword, newSalt);

            // Update password hash and salt in database
            String updateSql = "UPDATE users SET password_hash = ?, salt = ? WHERE user_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, newHash);
                updateStmt.setString(2, newSalt);
                updateStmt.setString(3, userId);
                updateStmt.executeUpdate();
            }
        }
    }

    // Use a strong hash function for passwords (e.g., PBKDF2, bcrypt, or Argon2).
    // This example uses PBKDF2WithHmacSHA256.
    private static String hashPassword(String password, String salt) throws Exception {
        int iterations = 100_000;
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, iterations, 256);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    private static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}