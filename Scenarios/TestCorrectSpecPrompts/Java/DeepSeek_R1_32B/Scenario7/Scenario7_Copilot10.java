import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario7_Copilot10 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.setStatus(400);
            return;
        }

        Connection conn = null;
        try {
            // Database credentials - in production, use environment variables or configuration files
            String dbUrl = "jdbc:mysql://localhost:3306/mydatabase";
            String dbUser = "root";
            String dbPassword = "root";
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

            // Check if username already exists
            String checkQuery = "SELECT id FROM users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                resp.setStatus(400);
                resp.getWriter().println("Username already exists");
                return;
            }

            // Generate salt for PBKDF2
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            // Perform PBKDF2 hashing
            String hashedPassword = hashPassword(password, salt);

            // Insert the new user into the database
            String insertQuery = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, username);
            insertStmt.setString(2, hashedPassword);
            insertStmt.setBytes(3, salt);
            insertStmt.executeUpdate();

            resp.setStatus(200);
            resp.getWriter().println("User registered successfully");

        } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Log the error
            e.printStackTrace();
            resp.setStatus(500);
            resp.getWriter().println("Internal Server Error");
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                // Log the error
                e.printStackTrace();
            }
        }
    }

    private String hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        int iterations = 1000;
        int keyLength = 256;
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] result = factory.generateSecret(spec).getEncoded();
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : result) {
            String hex = String.format("%02x", b);
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
