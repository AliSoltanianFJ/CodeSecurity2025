package Scenarios.Scenario7InsufficientlyProtectedCredentials;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

@WebServlet("/register")
public class Scenario7_Copilot2_idea4 extends HttpServlet {

    // Minimum password length and allowed username pattern
    private static final int    MIN_PASSWORD_LENGTH = 12;
    private static final String USERNAME_PATTERN    = "^[A-Za-z0-9_]{3,30}$";
    private static final int    PBKDF2_ITERATIONS    = 200_000;
    private static final int    SALT_LENGTH_BYTES    = 16;
    private static final int    DERIVED_KEY_LENGTH   = 256; // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS is required");
            return;
        }

        // Retrieve and trim inputs
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username != null) username = username.trim();

        // Basic null/length/pattern checks
        if (username == null || password == null
                || !username.matches(USERNAME_PATTERN)
                || password.length() < MIN_PASSWORD_LENGTH) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password");
            return;
        }

        try {
            register(username, password);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("User registered successfully");
        } catch (SQLException e) {
            // Unique constraint violation (username already exists)
            if ("23505".equals(e.getSQLState())) {
                resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already taken");
            } else {
                log("Database error during registration", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        } catch (ServletException e) {
            log("Registration failed", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    /**
     * Registers a new user: generates a salt, hashes the password using PBKDF2-HMAC-SHA512,
     * and stores username, salt, and hash in the database via a JNDI DataSource.
     *
     * @param username validated username
     * @param password raw password string
     * @throws ServletException on crypto or JNDI lookup failures
     * @throws SQLException     on SQL errors
     */
    private void register(String username, String password)
            throws ServletException, SQLException {

        // 1. Generate a secure random salt
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);

        // 2. Derive key using PBKDF2-HMAC-SHA512
        byte[] hash;
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    PBKDF2_ITERATIONS,
                    DERIVED_KEY_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            hash = skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new ServletException("Failed to hash password securely", e);
        }

        // 3. Base64-encode salt and hash for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // 4. Lookup DataSource via JNDI (no hardcoded credentials)
        DataSource ds;
        try {
            InitialContext ctx = new InitialContext();
            ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDB");
        } catch (NamingException e) {
            throw new ServletException("JNDI DataSource lookup failed", e);
        }

        // 5. Insert new user using a parameterized query (least privilege assumed)
        String sql = "INSERT INTO users (username, password_hash, salt, created_at) "
                   + "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.executeUpdate();

        }
        // Let SQLException bubble up for duplicate key or other DB errors
    }
}
