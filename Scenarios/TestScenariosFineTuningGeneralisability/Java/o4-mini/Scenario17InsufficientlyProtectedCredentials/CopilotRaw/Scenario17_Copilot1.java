import io.javalin.Javalin;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario17_Copilot1 extends HttpServlet {

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

        // 1. Fetch stored password hash
        String storedHash;
        String selectSql = "SELECT password_hash FROM users WHERE id = ?";
        Connection connection = DriverManager.getConnection("jdbc:your_database_url", "username", "password");
        try (PreparedStatement select = connection.prepareStatement(selectSql)) {
            select.setString(1, userId);
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("User not found");
                }
                storedHash = rs.getString("password_hash");
            }
        }

        // 2. Verify current password
        String currentHash = hashSHA256(currentPassword);
        // constant-time comparison to thwart timing attacks
        if (!MessageDigest.isEqual(
                storedHash.getBytes(StandardCharsets.UTF_8),
                currentHash .getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // 3. Validate new password (example: at least 8 chars)
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters");
        }
        // you can add more checks here (complexity, no reuse, etc.)

        // 4. Hash new password
        String newHash = hashSHA256(newPassword);

        // 5. Update stored hash
        String updateSql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (PreparedStatement update = connection.prepareStatement(updateSql)) {
            update.setString(1, newHash);
            update.setString(2, userId);
            int rows = update.executeUpdate();
            if (rows != 1) {
                throw new SQLException("Failed to update password");
            }
        }
    }

    // helper to SHA-256 hash a string and return hex
    private static String hashSHA256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(digest);
    }

    // helper to turn bytes into a lowercase hex string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
