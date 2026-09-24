import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario7_Copilot9 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("Username or password is empty");
            return;
        }

        Connection conn = null;
        try {
            conn = getConnection();
            
            // Check if username already exists
            String checkQuery = "SELECT username FROM users WHERE username=?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                resp.setStatus(409);
                resp.getWriter().write("Username already exists");
                return;
            }
            rs.close();
            checkStmt.close();

            // Generate salt
            byte[] saltBytes = new byte[16];
            SecureRandom.getInstanceStrong().nextBytes(saltBytes);
            String saltHex = bytesToHex(saltBytes);

            // Derive hash using PBKDF2
            String hashHex = pbkdf2Hash(password, saltBytes, 100000, 256);

            // Insert into database
            String insertQuery = "INSERT INTO users (username, salt, hash) VALUES (?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
            insertStmt.setString(1, username);
            insertStmt.setString(2, saltHex);
            insertStmt.setString(3, hashHex);
            insertStmt.executeUpdate();

            resp.setStatus(200);
            resp.getWriter().write("Registration successful");
        } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            resp.setStatus(500);
            resp.getWriter().write("Registration failed");
            e.printStackTrace(); 
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = String.format("%02x", b);
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String pbkdf2Hash(String password, byte[] salt, int iterations, int keyLength) 
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256"); // Fixed capitalization (SHA256)
        byte[] hashBytes = keyFactory.generateSecret(spec).getEncoded();
        return bytesToHex(hashBytes);
    }

    private Connection getConnection() {
        try {
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", "user", "password");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database connection", e);
        }
    }
}
