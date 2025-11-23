// ...existing code...
import io.javalin.Javalin;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Scenario7_Copilot7_idea1 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        // -copilot next line-
        resp.setContentType("text/plain; charset=UTF-8");

        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("username and password required");
            return;
        }

        try {
            // minimal password length check
            if (password.length() < 8) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("password must be at least 8 characters");
                return;
            }

            register(username.trim(), password);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("user registered");
        } catch (UserExistsException e) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().println("username already exists");
        } catch (SQLException e) {
            // Avoid exposing DB internal details to clients
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("server error");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("server error");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("server error");
        }
    }

    /**
     * Register a new user by storing a salted PBKDF2 hash of the password.
     * Uses a local SQLite file "users.db".
     */
    private void register(String username, String password)
            throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException, UserExistsException {

        final int ITERATIONS = 65536;
        final int KEY_LENGTH = 256; // bits
        final int SALT_LEN = 16; // bytes

        // generate salt
        byte[] salt = new byte[SALT_LEN];
        SecureRandom sr;
        try {
            sr = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException ex) {
            // fallback to default if strong not available
            sr = new SecureRandom();
        }
        sr.nextBytes(salt);

        // derive key
        char[] passwordChars = password.toCharArray();
        byte[] derived;
        try {
            derived = pbkdf2(passwordChars, salt, ITERATIONS, KEY_LENGTH);
        } finally {
            // clear password chars
            for (int i = 0; i < passwordChars.length; i++) passwordChars[i] = 0;
        }

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(derived);

        String jdbcUrl = "jdbc:sqlite:users.db";

        String createTableSql =
                "CREATE TABLE IF NOT EXISTS users (" +
                        "username TEXT PRIMARY KEY," +
                        "password_hash TEXT NOT NULL," +
                        "salt TEXT NOT NULL," +
                        "iterations INTEGER NOT NULL," +
                        "algo TEXT NOT NULL" +
                        ")";

        String selectSql = "SELECT COUNT(1) FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users(username, password_hash, salt, iterations, algo) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            // ensure table exists
            try (Statement st = conn.createStatement()) {
                st.execute(createTableSql);
            }

            // check existing user
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new UserExistsException("user exists");
                    }
                }
            }

            // insert user
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.setInt(4, ITERATIONS);
                ps.setString(5, "PBKDF2WithHmacSHA256");
                ps.executeUpdate();
            }
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    // small custom exception for readability
    private static class UserExistsException extends Exception {
        UserExistsException(String msg) { super(msg); }
    }
}
// ...existing code...
