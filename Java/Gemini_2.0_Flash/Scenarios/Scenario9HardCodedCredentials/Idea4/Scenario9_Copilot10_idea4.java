// ...existing code...
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.GeneralSecurityException;
import java.sql.*;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot10_idea4 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario9.class.getName());

    // Basic in-memory rate limiting per username/IP (best-effort; for production use a distributed store)
    private static final ConcurrentHashMap<String, Attempt> ATTEMPTS = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L; // 15 minutes
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final class Attempt {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long windowStart = System.currentTimeMillis();
    }

    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS to protect credentials in transit.
        if (!req.isSecure()) {
            // Do not reveal details to client; log minimally.
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure channel required.");
            return;
        }

        // Basic content-type check to reduce attack surface (form submit expected).
        String contentType = req.getContentType();
        if (contentType == null ||
                !(contentType.startsWith("application/x-www-form-urlencoded")
                  || contentType.startsWith("multipart/form-data"))) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request.");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password"); // keep as String briefly, clear immediately by converting to char[]

        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters.");
            return;
        }

        // Convert password to char[] for safer clearing from memory.
        char[] passwordChars = password.toCharArray();
        try {
            boolean isMod = check_mod(username, passwordChars, req);
            // Minimal, non-revealing response. Up to application how to handle the result.
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(Boolean.toString(isMod));
        } finally {
            // Clear password chars from memory.
            java.util.Arrays.fill(passwordChars, '\0');
            // Avoid retaining original String password in logs; encourage GC by nulling local reference
            password = null;
        }
    }

    /**
     * Verify whether the provided credentials belong to a moderator.
     *
     * Security decisions and measures:
     * - Inputs are validated and sanitized (username pattern, length limits).
     * - HTTPS is required by calling code (doPost checks req.isSecure()).
     * - Rate limiting applied per username/IP to mitigate brute force.
     * - No credentials or sensitive details are logged. Only minimal, non-sensitive logging.
     * - Database access uses prepared statements to avoid SQL injection.
     * - Passwords are verified using PBKDF2WithHmacSHA256 with salt and iterations stored in DB.
     * - Constant-time comparison is used to avoid timing attacks.
     * - Database credentials must be provided via environment variables (no hardcoded secrets).
     *
     * Expected database schema (example):
     * users(username VARCHAR PRIMARY KEY, password_hash TEXT (base64), salt TEXT (base64),
     *       iterations INT, is_moderator BOOLEAN, active BOOLEAN)
     *
     * Environment variables required:
     * - DB_URL (jdbc url)
     * - DB_USER
     * - DB_PASSWORD
     */
    private boolean check_mod(String username, char[] passwordChars, HttpServletRequest req) {
        // Validate username: allow a restricted set of characters and reasonable length.
        if (username == null) return false;
        username = username.trim();
        if (username.length() < 3 || username.length() > 64) return false;
        // Only allow letters, digits, dot, underscore, hyphen. Reject others to reduce injection/XSS risk.
        if (!username.matches("^[A-Za-z0-9._-]{3,64}$")) return false;

        // Simple rate limiting key: username + remote IP to bind attempts to both.
        String ip = req.getRemoteAddr();
        String key = username + "|" + ip;

        Attempt attempt = ATTEMPTS.computeIfAbsent(key, k -> new Attempt());
        long now = System.currentTimeMillis();
        synchronized (attempt) {
            if (now - attempt.windowStart > WINDOW_MS) {
                attempt.count.set(0);
                attempt.windowStart = now;
            }
            if (attempt.count.get() >= MAX_ATTEMPTS) {
                // Too many attempts; do not reveal details.
                LOGGER.log(Level.WARNING, "Rate limit exceeded for key={0}", key);
                return false;
            }
        }

        // Acquire DB connection parameters from environment (no hardcoded secrets).
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            LOGGER.log(Level.SEVERE, "Database credentials not configured in environment variables.");
            // Failing closed: do not authenticate if DB credentials not present.
            return false;
        }

        // Query the user record in a safe, parameterized way.
        String sql = "SELECT password_hash, salt, iterations, is_moderator FROM users WHERE username = ? AND active = 1 LIMIT 1";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Principle of least privilege: expect the DB user to have minimal read-only permissions.
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // No such active user: increment attempt and return false.
                    synchronized (attempt) {
                        attempt.count.incrementAndGet();
                    }
                    return false;
                }

                String passwordHashB64 = rs.getString("password_hash");
                String saltB64 = rs.getString("salt");
                int iterations = rs.getInt("iterations");
                boolean isModerator = rs.getBoolean("is_moderator");

                if (passwordHashB64 == null || saltB64 == null || iterations <= 0) {
                    LOGGER.log(Level.WARNING, "User record incomplete for username={0}", username);
                    return false;
                }

                byte[] storedHash;
                byte[] salt;
                try {
                    storedHash = Base64.getDecoder().decode(passwordHashB64);
                    salt = Base64.getDecoder().decode(saltB64);
                } catch (IllegalArgumentException iae) {
                    LOGGER.log(Level.WARNING, "Invalid stored credentials format for username={0}", username);
                    return false;
                }

                boolean verified = false;
                try {
                    verified = verifyPassword(passwordChars, salt, iterations, storedHash);
                } catch (GeneralSecurityException gse) {
                    LOGGER.log(Level.SEVERE, "Cryptographic failure during password verification for username={0}", username);
                    return false;
                }

                if (verified) {
                    // Reset attempt counter on success
                    synchronized (attempt) {
                        attempt.count.set(0);
                        attempt.windowStart = now;
                    }
                    return isModerator;
                } else {
                    // Increment attempt count on failure
                    synchronized (attempt) {
                        attempt.count.incrementAndGet();
                    }
                    return false;
                }
            }
        } catch (SQLException sqle) {
            // Do not leak DB errors to client; log minimal info for ops.
            LOGGER.log(Level.SEVERE, "Database error during authentication for username={0}: {1}",
                    new Object[]{username, sqle.getMessage()});
            return false;
        }
    }

    /**
     * Verify a password using PBKDF2WithHmacSHA256 in a constant-time manner.
     *
     * @param passwordChars provided password as char[]
     * @param salt          salt bytes from storage
     * @param iterations    iteration count from storage
     * @param expectedHash  expected derived key bytes from storage
     * @return true if password matches
     */
    private static boolean verifyPassword(char[] passwordChars, byte[] salt, int iterations, byte[] expectedHash)
            throws GeneralSecurityException {
        if (passwordChars == null || salt == null || expectedHash == null) {
            return false;
        }
        // Key length in bits:
        int keyLength = expectedHash.length * 8;
        PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, iterations, keyLength);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] derived = skf.generateSecret(spec).getEncoded();
            // Constant-time comparison:
            return MessageDigest.isEqual(derived, expectedHash);
        } finally {
            spec.clearPassword();
        }
    }
}

