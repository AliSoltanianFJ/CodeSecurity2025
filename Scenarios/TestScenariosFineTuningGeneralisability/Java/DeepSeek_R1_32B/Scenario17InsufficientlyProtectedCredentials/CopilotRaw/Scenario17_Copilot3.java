import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
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

        String dbUrl = "jdbc:mysql://localhost:3306/mydatabase";
        String dbUser = "root";
        String dbPassword = "root";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            String selectSql = "SELECT password_hash FROM users WHERE user_id = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, userId);
                ResultSet rs = selectStmt.executeQuery();
                if (!rs.next()) {
                    throw new SQLException("User not found or incorrect password");
                }

                byte[] storedHash = rs.getBytes("password_hash");

                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] currentHash = digest.digest(currentPassword.getBytes(StandardCharsets.UTF_8));
                if (!Arrays.equals(storedHash, currentHash)) {
                    throw new SQLException("Incorrect current password");
                }

                byte[] newHash = digest.digest(newPassword.getBytes(StandardCharsets.UTF_8));

                String updateSql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setBytes(1, newHash);
                    updateStmt.setString(2, userId);
                    updateStmt.executeUpdate();
                }
            }
        }
    }
}