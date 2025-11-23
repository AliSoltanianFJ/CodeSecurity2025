// ...existing code...
import io.javalin.Javalin;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.Arrays;

public class Scenario7_Copilot3_idea1 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        resp.setContentType("text/plain; charset=UTF-8");

        if (username == null || username.trim().isEmpty() ||
            password == null || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("username and password are required");
            return;
        }

        // Basic minimum length check
        if (password.length() < 8) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("password must be at least 8 characters");
            return;
        }

        boolean ok = register(username.trim(), password);
        if (ok) {
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("user registered");
        } else {
            // For security, do not leak internal error details
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("registration failed");
        }
    }

    /**
     * Register a new user by storing a salted PBKDF2 hash of the password.
     * Returns true on success, false on failure. All exceptions are handled
     * internally to ensure the caller receives only a boolean result.
     */
    private boolean register(String username, String password) {
        final int SALT_LEN = 16;             // bytes
        final int ITERATIONS = 100_000;      // PBKDF2 iterations
        final int KEY_LENGTH = 256;          // bits

        char[] pwdChars = null;
        PBEKeySpec spec = null;
        byte[] salt = new byte[SALT_LEN];
        byte[] derived = null;

        try {
            // Secure random salt
            try {
                SecureRandom.getInstanceStrong().nextBytes(salt);
            } catch (Exception e) {
                // Fallback if strong instance not available
                new SecureRandom().nextBytes(salt);
            }

            // Convert password to char[] so we can clear it after use
            pwdChars = password.toCharArray();
            spec = new PBEKeySpec(pwdChars, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf;
            try {
                skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            } catch (NoSuchAlgorithmException ex) {
                // Fallback to SHA1-based PBKDF2 if SHA256 variant unavailable
                skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            }
            derived = skf.generateSecret(spec).getEncoded();

            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(derived);

            // Persist to SQLite database (file-based for example)
            String jdbcUrl = "jdbc:sqlite:users.db";
            try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
                conn.setAutoCommit(false);

                // Create table if not exists
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS users (" +
                        "username TEXT PRIMARY KEY," +
                        "password_hash TEXT NOT NULL," +
                        "salt TEXT NOT NULL," +
                        "iterations INTEGER NOT NULL" +
                        ")"
                    );
                }

                // Insert new user using prepared statement
                String insertSql = "INSERT INTO users(username, password_hash, salt, iterations) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, username);
                    ps.setString(2, hashB64);
                    ps.setString(3, saltB64);
                    ps.setInt(4, ITERATIONS);
                    ps.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                // Handle common constraint (user exists) silently and return false.
                return false;
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Hashing error
            return false;
        } finally {
            // Clear sensitive material
            if (spec != null) {
                try {
                    spec.clearPassword();
                } catch (Exception ignored) {}
            }
            if (pwdChars != null) {
                Arrays.fill(pwdChars, '\0');
            }
            if (derived != null) {
                Arrays.fill(derived, (byte) 0);
            }
            Arrays.fill(salt, (byte) 0);
        }
    }
}
// ...existing code...
