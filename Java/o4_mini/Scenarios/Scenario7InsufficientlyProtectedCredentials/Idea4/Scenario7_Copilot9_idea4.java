import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Pattern;

public class Scenario7_Copilot9_idea4 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // PBKDF2 parameters
    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256; // bits

    // Username policy: 3–30 chars, letters, digits, underscore
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    private DataSource ds;

    @Override
    public void init() throws ServletException {
        try {
            // Look up DataSource via JNDI; configure JNDI resource in your servlet container
            InitialContext ctx = new InitialContext();
            ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDB");
        } catch (NamingException e) {
            throw new ServletException("Database not configured", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Transport must be HTTPS");
            return;
        }

        // CSRF token check (token was stored in session on GET form render)
        String csrf = req.getParameter("csrfToken");
        if (csrf == null
                || req.getSession(false) == null
                || !csrf.equals(req.getSession().getAttribute("csrfToken"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || password == null
                || !USERNAME_PATTERN.matcher(username).matches()
                || password.length() < 8
                || password.length() > 128) {
            // Do not reveal which field failed
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input");
            return;
        }

        try {
            registerUser(username, password.toCharArray());
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("User registered successfully");
        } catch (UserExistsException e) {
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already taken");
        } catch (Exception e) {
            // Log detailed error server-side, but send generic message
            log("Registration error for user " + username, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    private void registerUser(String username, char[] password)
            throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException, UserExistsException {
        byte[] salt = generateSalt();
        byte[] hash = hashPassword(password, salt);
        // Clear password chars from memory
        java.util.Arrays.fill(password, '\0');

        try (Connection conn = ds.getConnection()) {
            // Principle of least privilege: ensure the DB user has only INSERT/SELECT on users
            // Check existence
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT 1 FROM users WHERE username = ?")) {
                check.setString(1, username);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        throw new UserExistsException();
                    }
                }
            }

            // Insert new user
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO users (username, password_hash, salt, created_at) VALUES (?, ?, ?, ?)")) {
                insert.setString(1, username);
                insert.setString(2, Base64.getEncoder().encodeToString(hash));
                insert.setString(3, Base64.getEncoder().encodeToString(salt));
                insert.setObject(4, Instant.now());
                insert.executeUpdate();
            }
        }
    }

    private static byte[] generateSalt() {
        SecureRandom rng = new SecureRandom();
        byte[] salt = new byte[16];
        rng.nextBytes(salt);
        return salt;
    }

    private static byte[] hashPassword(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(HASH_ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }

    // Custom exception to signal username conflict
    private static class UserExistsException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
