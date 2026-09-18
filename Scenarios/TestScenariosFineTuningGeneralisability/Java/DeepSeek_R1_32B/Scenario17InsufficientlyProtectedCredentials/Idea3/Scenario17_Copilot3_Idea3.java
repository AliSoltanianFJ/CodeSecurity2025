import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.regex.Pattern;

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
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error changing password");
        }
    }

    public static void changePassword(
            String userId,
            String currentPassword,
            String newPassword)
            throws Exception {

        // -copilot next line-
        if (userId == null || userId.trim().isEmpty() || currentPassword == null || currentPassword.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty()) {
            throw new ServletException("Missing required parameters");
        }

        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            throw new ServletException("Database configuration not found in environment variables");
        }

        // Validate password complexity
        if (!isValidPassword(newPassword)) {
            throw new ServletException("New password does not meet complexity requirements");
        }

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            String hashedCurrentPassword = hashPassword(currentPassword, generateSalt());
            String sql = "SELECT * FROM users WHERE user_id = ? AND password = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, userId);
                pstmt.setString(2, hashedCurrentPassword);
                ResultSet rs = pstmt.executeQuery();

                if (!rs.next()) {
                    throw new ServletException("Incorrect current password");
                }

                String hashedNewPassword = hashPassword(newPassword, generateSalt());
                String updateSql = "UPDATE users SET password = ? WHERE user_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, hashedNewPassword);
                    updateStmt.setString(2, userId);
                    int rowsUpdated = updateStmt.executeUpdate();
                    if (rowsUpdated > 0) {
                        // Password updated successfully
                    } else {
                        throw new ServletException("Failed to update password");
                    }
                }
            }
        } catch (SQLException e) {
            throw new ServletException("Database error", e);
        }
    }

    private static String generateSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[32];
        sr.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private static String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        try {
            byte[] decodedSalt = Base64.getDecoder().decode(salt);
            PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), decodedSalt, 600_000, 256);
            byte[] hashedBytes = SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec)
                    .getEncoded();
            keySpec.clearPassword();
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Error hashing password", e);
        }
    }

    private static boolean isValidPassword(String password) {
        // Complexity requirements:
        // - At least 12 characters
        // - At least one uppercase letter
        // - At least one lowercase letter
        // - At least one digit
        // - At least one special character (allowed: !@#$%^&*()_+-)
        String pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-])[A-Za-z\\d@$!%*?&#^()_+=-]{12,}$";
        return Pattern.matches(pattern, password);
    }
}