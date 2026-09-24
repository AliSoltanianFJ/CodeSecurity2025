import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.DriverManager;

public class Scenario7_Copilot7 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("Username or password is missing");
            return;
        }

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/mydatabase", 
                    "dbuser", 
                    "dbpass");
        } catch (SQLException e) {
            throw new ServletException("Database connection failed", e);
        }

        try {
            // Check if username exists
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT username FROM users WHERE username = ?");
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                resp.setStatus(400);
                resp.getWriter().write("Username already exists");
                return;
            }

            // Generate salt
            byte[] salt = generateSalt();

            // Derive hash using PBKDF2
            String hash = deriveHash(password, salt);

            // Insert into database
            String query = "INSERT INTO users (username, salt, password_hash) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, HexFormat.of().formatHex(salt));
            stmt.setString(3, hash);
            stmt.executeUpdate();

            resp.setStatus(200);
            resp.getWriter().write("Registration successful");

        } catch (SQLException e) {
            throw new ServletException("Database error", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // Ignore on close
                }
            }
        }
    }

    private byte[] generateSalt() {
        try {
            SecureRandom random = SecureRandom.getInstanceStrong();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            return salt;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate salt", e);
        }
    }

    private String deriveHash(String password, byte[] salt) {
        int iterations = 1000;
        int keyLength = 256;
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] finalKey = factory.generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(finalKey);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("PBKDF2 generation failed", e);
        }
    }
}
