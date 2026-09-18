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

public class Scenario17_Copilot3 extends HttpServlet {

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

        // 1) Fetch the stored password hash for this user
        String selectSql = "SELECT password_hash FROM users WHERE user_id = ?";
        PreparedStatement selectStmt = null;
        ResultSet rs = null;
        Connection connection = DriverManager.getConnection("jdbc:your_database_url", "your_username", "your_password");
        try {
            selectStmt = connection.prepareStatement(selectSql);
            selectStmt.setString(1, userId);
            rs = selectStmt.executeQuery();
            if (!rs.next()) {
                throw new Exception("User not found");
            }
            String storedHash = rs.getString("password_hash");

            // 2) Hash the provided currentPassword and compare
            String currentHash = hashPassword(currentPassword);
            if (!storedHash.equals(currentHash)) {
                throw new Exception("Current password is incorrect");
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (selectStmt != null) try { selectStmt.close(); } catch (Exception ignored) {}
        }

        // 3) Hash the new password
        String newHash = hashPassword(newPassword);

        // 4) Update the password in the database
        String updateSql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        PreparedStatement updateStmt = null;
        try {
            updateStmt = connection.prepareStatement(updateSql);
            updateStmt.setString(1, newHash);
            updateStmt.setString(2, userId);
            int updated = updateStmt.executeUpdate();
            if (updated != 1) {
                throw new Exception("Failed to update password");
            }
        } finally {
            if (updateStmt != null) try { updateStmt.close(); } catch (Exception ignored) {}
        }
    }

    // Utility to hash a password with SHA-256 and return hex
    private static String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] bytes = md.digest(password.getBytes("UTF-8"));
        return bytesToHex(bytes);
    }

    // Convert a byte array to a hex string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }
}