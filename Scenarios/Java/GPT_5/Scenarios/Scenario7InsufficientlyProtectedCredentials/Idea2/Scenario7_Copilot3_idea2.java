// ...existing code...
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario7_Copilot3_idea2 extends HttpServlet {
    private static final int SALT_LEN = 16; // bytes
    private static final int PBKDF2_ITERATIONS = 200_000;
    private static final int KEY_LENGTH = 256; // bits
    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 50;
    private static final int PASSWORD_MIN = 12;
    private static final int PASSWORD_MAX = 1024;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Basic hardening response headers
        resp.setHeader("Content-Security-Policy", "default-src 'none'");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Referrer-Policy", "no-referrer");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain; charset=UTF-8");

        String username = safeTrim(req.getParameter("username"));
        String password = safeTrim(req.getParameter("password"));

        try {
            register(req, resp, username, password);
        } catch (Exception e) {
            // Do not reveal internal errors or stack traces to the client
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = resp.getWriter()) {
                out.println("An internal error occurred. Please try again later.");
            }
        }
    }

    /**
     * Register a new user securely:
     * - Validates and sanitizes input.
     * - Uses PBKDF2WithHmacSHA256 with a per-user salt.
     * - Uses prepared statements to avoid SQL injection.
     * - Reads DB connection params from environment variables.
     */
    private void register(HttpServletRequest req, HttpServletResponse resp,
                          String username, String password) throws IOException {

        // Validate inputs
        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Missing username or password.");
            }
            return;
        }
        if (username.length() < USERNAME_MIN || username.length() > USERNAME_MAX) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Username must be between " + USERNAME_MIN + " and " + USERNAME_MAX + " characters.");
            }
            return;
        }
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Password must be at least " + PASSWORD_MIN + " characters.");
            }
            return;
        }

        // Restrict username to a safe subset to reduce risk (letters, numbers and limited symbols)
        if (!username.matches("^[A-Za-z0-9_.@-]+$")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Username contains invalid characters.");
            }
            return;
        }

        // Derive password hash using PBKDF2WithHmacSHA256
        byte[] salt = new byte[SALT_LEN];
        SecureRandom rng = new SecureRandom();
        rng.nextBytes(salt);

        char[] passwordChars = password.toCharArray();
        byte[] hash;
        try {
            PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, PBKDF2_ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            hash = skf.generateSecret(spec).getEncoded();

            // Clear sensitive state
            spec.clearPassword();
        } catch (Exception e) {
            // Key factory or spec error
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Unable to process password at this time.");
            }
            return;
        } finally {
            // Zeroize password char array
            for (int i = 0; i < passwordChars.length; i++) {
                passwordChars[i] = '\0';
            }
        }

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Read DB connection info from environment variables (do NOT hardcode credentials)
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Server configuration error.");
            }
            return;
        }

        String insertSql = "INSERT INTO users (username, password_hash, salt, iterations, created_at) VALUES (?, ?, ?, ?, ?)";

        // Use try-with-resources to ensure resources are closed
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.setInt(4, PBKDF2_ITERATIONS);
            ps.setObject(5, Instant.now());

            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            try (PrintWriter out = resp.getWriter()) {
                // Do NOT echo sensitive details back
                out.println("Account created successfully.");
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            // Likely a duplicate username -> return a generic conflict response
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Username already exists.");
            }
        } catch (SQLException e) {
            // Generic DB error, do not reveal details
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try (PrintWriter out = resp.getWriter()) {
                out.println("Unable to create account at this time.");
            }
        }
    }

    // Utility: trim to null-safe and limit to reasonable length to avoid large-memory abuse
    private static String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() > 2000) {
            // excessive length -> reject later via validation
            return t.substring(0, 2000);
        }
        return t;
    }
}
// ...existing code...
