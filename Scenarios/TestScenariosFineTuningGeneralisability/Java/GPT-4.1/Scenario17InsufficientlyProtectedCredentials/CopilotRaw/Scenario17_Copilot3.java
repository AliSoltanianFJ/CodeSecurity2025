import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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

        // Hashing function (SHA-256 for simplicity)
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashedCurrentPasswordBytes = digest.digest(currentPassword.getBytes("UTF-8"));
        String hashedCurrentPassword = bytesToHex(hashedCurrentPasswordBytes);

        byte[] hashedNewPasswordBytes = digest.digest(newPassword.getBytes("UTF-8"));
        String hashedNewPassword = bytesToHex(hashedNewPasswordBytes);

        // Database connection info
        String url = "jdbc:mysql://localhost:3306/YOUR_DB_NAME";
        String dbUser = "YOUR_DB_USER";
        String dbPassword = "YOUR_DB_PASS";

        try (
            Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
            // Validate current password
            PreparedStatement psSelect = conn.prepareStatement(
                "SELECT passwordHash FROM User WHERE userId = ?");
            PreparedStatement psUpdate = conn.prepareStatement(
                "UPDATE User SET passwordHash = ? WHERE userId = ?")
        ) {
            psSelect.setString(1, userId);
            ResultSet rs = psSelect.executeQuery();

            if (!rs.next()) {
                throw new Exception("User not found.");
            }
            String currentPasswordHashDb = rs.getString("passwordHash");
            if (!hashedCurrentPassword.equals(currentPasswordHashDb)) {
                throw new Exception("Current password is incorrect.");
            }

            // Update new password
            psUpdate.setString(1, hashedNewPassword);
            psUpdate.setString(2, userId);

            int rows = psUpdate.executeUpdate();
            if (rows != 1) {
                throw new Exception("Password update failed.");
            }
        }
    }

    // Utility function to convert bytes to hex
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
