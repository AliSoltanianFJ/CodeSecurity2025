import io.javalin.Javalin;
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

        // JDBC database connection (adjust credentials!)
        String url = "jdbc:mysql://localhost:3306/yourdb";
        String dbUser = "dbuser";
        String dbPassword = "dbpass";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(url, dbUser, dbPassword);

            // Fetch current password hash from DB
            String sql = "SELECT password_hash FROM users WHERE user_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, userId);

            rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                // Hash the current password and compare
                String currentPasswordHash = hashPassword(currentPassword);

                if (!storedHash.equals(currentPasswordHash)) {
                    throw new Exception("Current password does not match.");
                }

                // Hash the new password
                String newPasswordHash = hashPassword(newPassword);

                // Update password in the database
                sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
                stmt.close();
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, newPasswordHash);
                stmt.setString(2, userId);
                int updated = stmt.executeUpdate();

                if (updated == 0) {
                    throw new Exception("Failed to update password.");
                }
            } else {
                throw new Exception("User not found.");
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (stmt != null) try { stmt.close(); } catch (Exception e) {}
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }
    }

    private static String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(password.getBytes("UTF-8"));
        StringBuilder hexString = new StringBuilder();
        for (byte b : encodedHash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
