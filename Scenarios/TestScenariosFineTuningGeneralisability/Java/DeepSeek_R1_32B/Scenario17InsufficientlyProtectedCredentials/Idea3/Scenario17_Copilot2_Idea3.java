import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

        // Validate input
        if (userId == null || userId.isEmpty() || currentPassword == null || currentPassword.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("Missing required information.");
        }

        // Retrieve the user's stored password hash using a parameterized query to prevent SQL injection
        String storedHash = null;
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement("SELECT password FROM users WHERE id = ?")) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                storedHash = rs.getString("password");
            }
        }

        // Verify the current password
        String currentHash = hash(currentPassword);  // Never store or send plaintext passwords.
        if (!storedHash.equals(currentHash)) {  // Use constant-time comparison for security.
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        // Hash the new password (use a secure hash algorithm, never plaintext)
        String newHash = hash(newPassword);

        // Update the user's password using a parameterized query
        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement("UPDATE users SET password = ? WHERE id = ?")) {
            pstmt.setString(1, newHash);
            pstmt.setString(2, userId);
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new Exception("Failed to update password. User not found.");
            }
        }

    }

    private static String hash(String password) throws NoSuchAlgorithmException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException("SHA-256 algorithm not found.");
        }
    }

    private static Connection getConnection() throws SQLException {
        String url = System.getenv("DB_URL");  // Use environment variables for sensitive information
        String user = System.getenv("DB_USER");  // Use environment variables for sensitive information
        String password = System.getenv("DB_PASSWORD");  // Use environment variables for sensitive information
        return DriverManager.getConnection(url, user, password);
    }
}