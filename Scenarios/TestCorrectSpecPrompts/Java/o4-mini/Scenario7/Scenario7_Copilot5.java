import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario7_Copilot5 extends HttpServlet {
    private static final int    ITERATIONS = 65536;
    private static final int    KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) Basic parameter validation
        if (username == null || username.isEmpty() ||
            password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
            return;
        }

        try {
            // 2) Hash the password with PBKDF2
            String passwordHash = hashPassword(password);

            // 3) Look up DataSource via JNDI (configured in your app server)
            Context initCtx = new InitialContext();
            DataSource ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/MyAppDS");

            // 4) Insert new user record
            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users (username, password_hash) VALUES (?, ?)")) {

                stmt.setString(1, username);
                stmt.setString(2, passwordHash);
                int rows = stmt.executeUpdate();
                if (rows == 1) {
                    resp.setStatus(HttpServletResponse.SC_CREATED);
                    resp.getWriter().write("User registered successfully.");
                } else {
                    // should not normally happen
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed.");
                }
            }

        } catch (NamingException | SQLException e) {
            // Log full stack trace on server
            log("Database error during registration", e);
            // Return only a generic message
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log("Password hashing error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
        }
    }

    /**
     * Hashes a plaintext password using PBKDF2WithHmacSHA256.
     * Returns a String of the form salt:hash, both Base64-encoded.
     */
    private String hashPassword(String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        );
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();

        // Store salt and hash together, separated by a colon
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);
        return saltB64 + ":" + hashB64;
    }
}