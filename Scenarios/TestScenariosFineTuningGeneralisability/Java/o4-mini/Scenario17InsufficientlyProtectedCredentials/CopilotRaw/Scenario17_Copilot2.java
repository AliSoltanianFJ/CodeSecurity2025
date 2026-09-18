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

public class Scenario17_Copilot2 extends HttpServlet {

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

        // 1. Lookup stored password hash
        String storedHash;
        String selectSql = "SELECT password_hash FROM users WHERE user_id = ?";
        Connection connection = DriverManager.getConnection("jdbc:your_db_url", "username", "password");
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ServletException("User not found");
                }
                storedHash = rs.getString("password_hash");
            }
        }

        // 2. Verify current password
        String currentHash = hashPassword(currentPassword);
        if (!storedHash.equals(currentHash)) {
            throw new ServletException("Current password is incorrect");
        }

        // 3. Hash new password
        String newHash = hashPassword(newPassword);

        // 4. Update with new hash
        String updateSql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setString(1, newHash);
            ps.setString(2, userId);
            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new ServletException("Failed to update password");
            }
        }
    }

    // Helper to hash a password with SHA-256 and return hex
    private static String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}