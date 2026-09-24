import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario7_Copilot2 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            sendErrorResponse(resp, "Username or password is missing", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(
                    System.getenv("DB_URL"),
                    System.getenv("DB_USER"),
                    System.getenv("DB_PASS")
            );
        } catch (SQLException e) {
            sendErrorResponse(resp, "Database connection failed", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        try (PreparedStatement checkStmt = connection.prepareStatement("SELECT username FROM users WHERE username = ?")) {
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    sendErrorResponse(resp, "Username already exists", HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
        } catch (SQLException e) {
            sendErrorResponse(resp, "Error checking username", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // Generate salt
        byte[] salt = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);

        // Generate hashed password
        byte[] hashedPassword = null;
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
            SecretKey key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec);
            hashedPassword = key.getEncoded();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            sendErrorResponse(resp, "Error hashing password", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // Convert to Base64 strings
        String saltStr = Base64.getEncoder().encodeToString(salt);
        String hashStr = Base64.getEncoder().encodeToString(hashedPassword);

        // Insert into database
        String insertQuery = "INSERT INTO users (username, salt, password_hash) VALUES (?, ?, ?)";
        try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
            insertStmt.setString(1, username);
            insertStmt.setString(2, saltStr);
            insertStmt.setString(3, hashStr);
            insertStmt.executeUpdate();
        } catch (SQLException e) {
            sendErrorResponse(resp, "Registration failed", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    // Ignore on close
                }
            }
        }

        // Send success response
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().write("{ \"status\": \"success\", \"message\": \"Registration successful\" }");
    }

    private void sendErrorResponse(HttpServletResponse resp, String message, int statusCode) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json");
        resp.getWriter().write("{ \"status\": \"error\", \"message\": \"" + message + "\" }");
    }
}
