import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario17_Copilot1 extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(Scenario17_Copilot1.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String userId = request.getParameter("userId");
        String currentPassword = request.getParameter("currentPassword");   
        String newPassword = request.getParameter("newPassword");
        try {
            changePassword(userId, currentPassword, newPassword);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error changing password", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error changing password");
        }
    }

    public static void changePassword(
            String userId,
            String currentPassword,
            String newPassword)
            throws Exception {

        String currentHash = createHash(currentPassword);
        String newHash = createHash(newPassword);

        Connection conn = DriverManager.getConnection(
            "your-database-url",
            "your-database-username",
            "your-database-password");

        try {
            PreparedStatement checkStmt = conn.prepareStatement("SELECT user_id FROM users WHERE user_id = ? AND password = ?");
            checkStmt.setString(1, userId);
            checkStmt.setString(2, currentHash);
            ResultSet rs = checkStmt.executeQuery();
            if (!rs.next()) {
                throw new Exception("Incorrect current password or user not found");
            }
            rs.close();
            checkStmt.close();

            PreparedStatement updateStmt = conn.prepareStatement("UPDATE users SET password = ? WHERE user_id = ?");
            updateStmt.setString(1, newHash);
            updateStmt.setString(2, userId);
            updateStmt.executeUpdate();
            updateStmt.close();

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Failed to close database connection", e);
                }
            }
        }
    }

    private static String createHash(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = String.format("%02x", b);
            hexString.append(hex);
        }
        return hexString.toString();
    }
}