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

    // Ensure you have a JDBC driver and update the DB connection details accordingly.
    // For demonstration purposes ONLY. In production, use stronger hashing (e.g., bcrypt) and secure secrets.

    public static void changePassword(
            String userId,
            String currentPassword,
            String newPassword)
            throws Exception {

        // Validate input (very basic, expand as needed)
        if (userId == null || currentPassword == null || newPassword == null) {
            throw new IllegalArgumentException("All fields are required.");
        }

        // Connect to the database (update URL, username, password as appropriate)
        String dbUrl = "jdbc:mysql://localhost:3306/testdb";
        String dbUser = System.getenv("DB_USER"); // Use environment variable for DB username
        String dbPass = System.getenv("DB_PASSWORD"); // Use environment variable for DB password

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);

            // Fetch the user's current password hash
            String selectSql = "SELECT password FROM users WHERE userId = ?";
            stmt = conn.prepareStatement(selectSql);
            stmt.setString(1, userId);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                throw new Exception("User not found.");
            }

            String storedHash = rs.getString("password");

            // Verify current password
            String currentPasswordHash = hashPassword(currentPassword);
            if (!storedHash.equals(currentPasswordHash)) {
                throw new Exception("Current password is incorrect.");
            }

            // Hash the new password
            String newPasswordHash = hashPassword(newPassword);

            // Update the password in the database
            String updateSql = "UPDATE users SET password = ? WHERE userId = ?";
            stmt.close();
            stmt = conn.prepareStatement(updateSql);
            stmt.setString(1, newPasswordHash);
            stmt.setString(2, userId);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new Exception("Password update failed.");
            }

            

        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (stmt != null) try { stmt.close(); } catch (Exception e) {}
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }
    }

    // VERY basic password hashing using SHA-256. In production, use bcrypt, Argon2, or PBKDF2!
    private static String hashPassword(String password) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
