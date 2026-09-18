import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

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

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // 1. Connect to DB
            conn = DriverManager.getConnection("jdbc:mysql://localhost/db", "user", "pass");

            // 2. Retrieve existing hash and salt
            stmt = conn.prepareStatement("SELECT password_hash, password_salt FROM users WHERE id = ?");
            stmt.setString(1, userId);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                throw new Exception("User not found");
            }
            String storedHash = rs.getString("password_hash");
            String storedSalt = rs.getString("password_salt");

            // 3. Hash the current password with stored salt
            String currentHash = hashPassword(currentPassword, Base64.getDecoder().decode(storedSalt));
            if (!currentHash.equals(storedHash)) {
                throw new Exception("Current password incorrect");
            }

            // 4. Generate new salt and hash for new password
            byte[] newSalt = generateSalt();
            String newHash = hashPassword(newPassword, newSalt);

            // 5. Update with new password hash and salt
            stmt.close();
            stmt = conn.prepareStatement("UPDATE users SET password_hash = ?, password_salt = ? WHERE id = ?");
            stmt.setString(1, newHash);
            stmt.setString(2, Base64.getEncoder().encodeToString(newSalt));
            stmt.setString(3, userId);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new Exception("Failed to update password");
            }
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    // -- Helpers: --

    private static String hashPassword(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }
}
