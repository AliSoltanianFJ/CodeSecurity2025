import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.mindrot.jbcrypt.BCrypt;


public class Scenario17_Copilot1_Idea3 extends HttpServlet {

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

        // Use proper password hashing (e.g., BCrypt)
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // Get a database connection (update connection string as needed)
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/yourdb", System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"));

            // Retrieve current password hash from DB
            String selectSql = "SELECT password_hash FROM users WHERE user_id = ?";
            stmt = conn.prepareStatement(selectSql);
            stmt.setString(1, userId);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                throw new Exception("User not found.");
            }

            String storedHash = rs.getString("password_hash");

            // Verify current password
            if (!BCrypt.checkpw(currentPassword, storedHash)) {
                throw new Exception("Current password is incorrect.");
            }

            // Validate new password strength (example: at least 8 chars, etc.)
            if (newPassword == null || newPassword.length() < 8) {
                throw new Exception("New password must be at least 8 characters long.");
            }

            // Hash new password
            String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());

            // Update password in DB using parameterized queries
            String updateSql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
            stmt.close();
            stmt = conn.prepareStatement(updateSql);
            stmt.setString(1, newHash);
            stmt.setString(2, userId);
            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated == 0) {
                throw new Exception("Password update failed.");
            }
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
}

