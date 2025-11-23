// ...existing code...
import io.javalin.Javalin;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Scenario7_Copilot8_idea1 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        resp.setContentType("text/plain; charset=UTF-8");

        if (username == null || username.trim().isEmpty() ||
            password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "username and password required");
            return;
        }

        try {
            register(username.trim(), password);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("user registered");
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("unique") || msg.contains("constraint") || e.getErrorCode() == 19) {
                resp.sendError(HttpServletResponse.SC_CONFLICT, "username already exists");
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "database error");
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "server error");
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "server error");
        }
    }

    /**
     * Register a new user by storing a salted PBKDF2 hash of the password.
     * Uses a local SQLite file "users.db" for demonstration.
     */
    private void register(String username, String password)
            throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        final int iterations = 100_000;
        final int keyLength = 256; // bits
        final int saltLen = 16; // bytes

        // Generate a strong random salt (fallback to default SecureRandom if necessary)
        byte[] salt = new byte[saltLen];
        SecureRandom sr;
        try {
            sr = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException ex) {
            sr = new SecureRandom();
        }
        sr.nextBytes(salt);

        // Derive key using PBKDF2WithHmacSHA256
        char[] pwdChars = password.toCharArray();
        byte[] derived = null;
        PBEKeySpec spec = new PBEKeySpec(pwdChars, salt, iterations, keyLength);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            derived = skf.generateSecret(spec).getEncoded();
        } finally {
            // wipe sensitive data in memory where possible
            spec.clearPassword();
            Arrays.fill(pwdChars, '\0');
        }

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(derived);

        // attempt to zero derived bytes after encoding
        if (derived != null) {
            Arrays.fill(derived, (byte) 0);
        }
        Arrays.fill(salt, (byte) 0);

        String jdbcUrl = "jdbc:sqlite:users.db";

        String createTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                "username TEXT PRIMARY KEY, " +
                "password_hash TEXT NOT NULL, " +
                "salt TEXT NOT NULL, " +
                "iterations INTEGER NOT NULL" +
                ")";

        String insertSql = "INSERT INTO users(username, password_hash, salt, iterations) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(createTableSql);
            }

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.setInt(4, iterations);
                ps.executeUpdate();
            }

            conn.commit();
        }
    }
}
