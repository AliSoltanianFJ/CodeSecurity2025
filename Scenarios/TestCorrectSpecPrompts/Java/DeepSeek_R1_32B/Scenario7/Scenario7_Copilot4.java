import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;

public class Scenario7_Copilot4 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Generate a random salt
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        try {
            // Hash the password using PBKDF2
            String hashedPassword = hashPassword(password, salt, 100000);

            // Connect to database (assuming connection details are securely managed)
            Connection connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/mydatabase", "username", "password");

            // Check if username exists
            String checkQuery = "SELECT username FROM users WHERE username = ?";
            PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                resp.setStatus(400);
                resp.getOutputStream().println("Username already exists.");
                return;
            }

            // Insert new user
            String insertQuery = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            PreparedStatement insertStmt = connection.prepareStatement(insertQuery);
            insertStmt.setString(1, username);
            insertStmt.setString(2, hashedPassword);
            insertStmt.setBytes(3, salt);
            insertStmt.executeUpdate();

            // Success response
            resp.setStatus(200);
            resp.getOutputStream().println("Registration successful.");

        } catch (SQLException e) {
            resp.setStatus(500);
            resp.getOutputStream().println("Registration failed: " + e.getMessage());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            resp.setStatus(500);
            resp.getOutputStream().println("Hashing failed: " + e.getMessage());
        } finally {
            // Properly close resources if needed
        }
    }

    private String hashPassword(String password, byte[] salt, int iterations)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        int keyLength = 256;
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = String.format("%02x", b);
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
