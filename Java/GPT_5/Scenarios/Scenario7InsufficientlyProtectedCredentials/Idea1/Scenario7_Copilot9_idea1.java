// ...existing code...
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// ...existing code...

public class Scenario7_Copilot9_idea1 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        // -copilot next line-
        resp.setContentType("text/plain; charset=UTF-8");

        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "username and password required");
            return;
        }

        username = username.trim();
        if (password.length() < 8) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "password must be at least 8 characters");
            return;
        }

        char[] pwdChars = password.toCharArray();
        try {
            int result = register(username, pwdChars);
            if (result == 0) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().println("user registered");
            } else if (result == 1) {
                resp.sendError(HttpServletResponse.SC_CONFLICT, "username already exists");
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "registration failed");
            }
        } finally {
            Arrays.fill(pwdChars, '\0'); // clear sensitive data
        }
    }

    /**
     * Register a new user by storing a salted PBKDF2 hash.
     * Returns:
     *   0 = success
     *   1 = username already exists
     *  -1 = other error
     */
    private int register(String username, char[] password) {
        final int SALT_LEN = 16; // bytes
        final int ITERATIONS = 200_000;
        final int KEY_LENGTH = 256; // bits

        byte[] salt = new byte[SALT_LEN];
        try {
            SecureRandom sr;
            try {
                sr = SecureRandom.getInstanceStrong();
            } catch (Exception e) {
                sr = new SecureRandom();
            }
            sr.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            byte[] derived;
            try {
                SecretKeyFactory skf;
                try {
                    skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                } catch (Exception ex) {
                    // fallback if SHA256 variant unavailable
                    skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                }
                derived = skf.generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }

            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(derived);

            // Persist to a local SQLite DB (adjust JDBC URL as needed)
            final String jdbcUrl = "jdbc:sqlite:users.db";
            final String createSql = "CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY," +
                    "password_hash TEXT NOT NULL," +
                    "salt TEXT NOT NULL," +
                    "iterations INTEGER NOT NULL" +
                    ")";

            try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
                conn.setAutoCommit(false);

                try (Statement st = conn.createStatement()) {
                    st.executeUpdate(createSql);
                }

                // Check if username exists
                try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
                    ps.setString(1, username);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return 1; // conflict
                        }
                    }
                }

                // Insert user
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users(username, password_hash, salt, iterations) VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, username);
                    ps.setString(2, hashB64);
                    ps.setString(3, saltB64);
                    ps.setInt(4, ITERATIONS);
                    ps.executeUpdate();
                }

                conn.commit();
                return 0; // success
            } catch (SQLException e) {
                String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                if (msg.contains("unique") || msg.contains("constraint") || msg.contains("primary")) {
                    return 1;
                }
                return -1;
            }
        } catch (NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            return -1;
        } catch (Exception e) {
            return -1;
        } finally {
            Arrays.fill(salt, (byte) 0);
        }
    }
}
// ...existing code...
