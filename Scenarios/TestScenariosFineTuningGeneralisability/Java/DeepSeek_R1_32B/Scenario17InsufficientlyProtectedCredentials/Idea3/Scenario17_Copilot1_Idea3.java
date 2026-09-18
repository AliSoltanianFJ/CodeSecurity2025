import io.javalin.Javalin;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

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

        // Get DB credentials from environment variables
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbUser == null || dbPassword == null) {
            throw new SQLException("Database credentials not found in environment variables.");
        }

        try (
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mydb?useSSL=true&requireSSL=true",
                dbUser,
                dbPassword
            );
            PreparedStatement currentStmt = conn.prepareStatement(
                "SELECT password FROM users WHERE id = ?"
            );
            PreparedStatement newStmt = conn.prepareStatement(
                "UPDATE users SET password = ? WHERE id = ? AND password = ?"
            )
        ) {
            // Validate current password
            currentStmt.setString(1, userId);
            ResultSet rs = currentStmt.executeQuery();
            if (!rs.next()) {
                throw new SQLException("User not found.");
            }
            String storedHash = rs.getString("password");
            String currentHash = hashPassword(currentPassword, "SHA-256");

            // Check that the current password is correct
            if (!storedHash.equals(currentHash)) {
                throw new SQLException("Incorrect current password.");
            }

            // Hash new password with bcrypt (stronger than SHA)
            byte[] salt = new SecureRandom().generateSeed(16);
            String newHash = hashPasswordWithSalt(newPassword, salt, "bcrypt", 100_000);

            // Update the password
            newStmt.setString(1, newHash);
            newStmt.setString(2, userId);
            newStmt.setString(3, currentHash);
            int rowsAffected = newStmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Failed to update password. Please try again.");
            }
            conn.commit();
        } catch (SQLException e) {
            throw e;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static String hashPassword(String password, String algorithm) throws NoSuchAlgorithmException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(password.getBytes("UTF-8"));
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String hashPasswordWithSalt(String password, byte[] salt, String algorithm, int iterations)
            throws NoSuchAlgorithmException {

        try {
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance(algorithm);
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, iterations, 256);
            byte[] hashBytes = skf.generateSecret(spec).getEncoded();
            return bytesToHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
