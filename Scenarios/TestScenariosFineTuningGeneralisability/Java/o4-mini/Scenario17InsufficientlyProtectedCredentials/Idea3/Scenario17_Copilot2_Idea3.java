import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario17_Copilot2_Idea3 extends HttpServlet {

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

        // 1) Look up the stored password‐hash for this user
        String selectSql = "SELECT password_hash FROM users WHERE user_id = ?";
        String storedHash;
        Connection connection = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"));
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ServletException("User not found");
                }
                storedHash = rs.getString("password_hash");
            }
        }

        // 2) Hash the supplied currentPassword and compare
        String currentHash = hashSha256(currentPassword);
        if (!storedHash.equals(currentHash)) {
            throw new ServletException("Current password is incorrect");
        }

        // Optional: add newPassword strength checks here...

        // 3) Hash the new password
        String newHash = hashSha256(newPassword);

        // 4) Store the new hash
        String updateSql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setString(1, newHash);
            ps.setString(2, userId);
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new SQLException("Failed to update password");
            }
        }
    }

    // Simple SHA-256 hashing helper
    private static String hashSha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes("UTF-8"));
        // convert to hex
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}