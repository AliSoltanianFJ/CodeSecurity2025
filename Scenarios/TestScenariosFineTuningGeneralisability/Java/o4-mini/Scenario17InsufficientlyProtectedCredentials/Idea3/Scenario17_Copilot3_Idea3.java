import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.nio.charset.StandardCharsets;

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

        // 1) Fetch stored password hash for this user
        String fetchSql = "SELECT password_hash FROM users WHERE id = ?";
        String storedHash;
        Connection connection = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"));
        try (PreparedStatement ps = connection.prepareStatement(fetchSql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new Exception("User not found");
                }
                storedHash = rs.getString("password_hash");
            }
        }

        // 2) Verify the current password
        String currentHash = sha256(currentPassword);
        if (!storedHash.equals(currentHash)) {
            throw new Exception("Current password is incorrect");
        }

        // 3) Hash the new password
        String newHash = sha256(newPassword);

        // 4) Update the database
        String updateSql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setString(1, newHash);
            ps.setString(2, userId);
            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new Exception("Failed to update password");
            }
        }
    }

    // Utility to compute SHA-256 hex digest of a UTF-8 password
    private static String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(digest);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}