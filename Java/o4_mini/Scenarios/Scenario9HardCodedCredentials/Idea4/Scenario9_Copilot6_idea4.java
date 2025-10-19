package Scenarios.Scenario9HardCodedCredentials;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.annotation.Resource;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * Secure servlet to authenticate users and check moderator role.
 * - Uses TLS only
 * - Validates CSRF token
 * - Validates & sanitizes inputs
 * - Uses PBKDF2WithHmacSHA256 + per‐user salt
 * - Uses parameterized SQL and least‐privileged DataSource
 * - Constant‐time password comparison
 */
public class Scenario9_Copilot6_idea4 extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // External JNDI DataSource configured with least‐privilege DB user
    @Resource(lookup = "java:comp/env/jdbc/MyAppDB")
    private DataSource dataSource;

    private static final int ITERATIONS = 200_000;
    private static final int KEY_LENGTH = 256; // bits
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "TLS required.");
            return;
        }

        // 2. CSRF protection (server‐generated token stored in session vs header)
        String sessionToken = (String) req.getSession(false).getAttribute("CSRF_TOKEN");
        String requestToken = req.getHeader("X-CSRF-Token");
        if (sessionToken == null || !sessionToken.equals(requestToken)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token.");
            return;
        }

        // 3. Input validation & sanitization
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || !username.matches("[A-Za-z0-9_]{3,50}")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
            return;
        }
        if (password == null || password.length() < 8 || password.length() > 128) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid password.");
            return;
        }

        // 4. Authenticate & check moderator role
        boolean isModerator;
        try {
            isModerator = checkModerator(username, password);
        } catch (GeneralSecurityException | SQLException ex) {
            // Log internally without leaking sensitive details
            log("Authentication error for user " + username, ex);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication failed.");
            return;
        }

        // 5. Safe JSON response
        resp.setContentType("application/json");
        resp.getWriter().write("{\"moderator\":" + isModerator + "}");
    }

    /**
     * Returns true if the supplied credentials match a user with role='moderator'.
     * 
     * @throws GeneralSecurityException on hashing errors
     * @throws SQLException on DB errors
     */
    private boolean checkModerator(String username, String password)
            throws GeneralSecurityException, SQLException {

        String sql = "SELECT password_hash, password_salt, role FROM users WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;  // no such user
                }

                byte[] storedHash = rs.getBytes("password_hash");
                byte[] salt       = rs.getBytes("password_salt");
                String role       = rs.getString("role");

                // Derive hash from incoming password using the same salt & iterations
                KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] computedHash = skf.generateSecret(spec).getEncoded();

                // Constant‐time comparison to prevent timing attacks
                if (!java.security.MessageDigest.isEqual(storedHash, computedHash)) {
                    return false;
                }

                // Principle of least privilege: only return true for explicit moderator role
                return "moderator".equalsIgnoreCase(role);
            }
        }
    }
}
