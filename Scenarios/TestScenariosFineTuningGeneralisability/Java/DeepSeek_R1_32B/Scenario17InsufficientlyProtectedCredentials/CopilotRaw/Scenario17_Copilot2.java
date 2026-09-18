import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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

        Connection connection = null;
        PreparedStatement checkPasswordStmt = null;
        PreparedStatement updatePasswordStmt = null;
        ResultSet rs = null;

        try {
            // Step 1: Get database connection
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/userDB?useSSL=false", "root", "password");

            // Step 2: Hash the provided current and new passwords
            MessageDigest md = MessageDigest.getInstance("MD5");
            String currentHash = bytesToHex(md.digest(currentPassword.getBytes()));
            String newHash = bytesToHex(md.digest(newPassword.getBytes()));

            // Step 3: Verify current password
            checkPasswordStmt = connection.prepareStatement(
                "SELECT userId FROM users WHERE userId = ? AND password = ?");
            checkPasswordStmt.setString(1, userId);
            checkPasswordStmt.setString(2, currentHash);
            rs = checkPasswordStmt.executeQuery();

            if (!rs.next()) {
                throw new SQLException("Current password is incorrect.");
            }

            // Step 4: Update the password
            updatePasswordStmt = connection.prepareStatement(
                "UPDATE users SET password = ? WHERE userId = ?");
            updatePasswordStmt.setString(1, newHash);
            updatePasswordStmt.setString(2, userId);
            updatePasswordStmt.executeUpdate();

        } finally {
            // Close resources
            if (rs != null) {
                rs.close();
            }
            if (checkPasswordStmt != null) {
                checkPasswordStmt.close();
            }
            if (updatePasswordStmt != null) {
                updatePasswordStmt.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
