// ...existing code...
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario9_Copilot7_idea2 extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(Scenario9.class.getName());
    private static final Pattern USERNAME_RE = Pattern.compile("^[A-Za-z0-9._-]{1,50}$");
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final int MIN_PBKDF2_ITERATIONS = 10000;

    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic validation / sanitisation of input
        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        username = username.trim();
        if (!USERNAME_RE.matcher(username).matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (password.length() == 0 || password.length() > MAX_PASSWORD_LENGTH) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        boolean isMod = check_mod(username, password);

        if (isMod) {
            // Do NOT echo username or sensitive data.
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("access granted");
        } else {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("access denied");
        }
    }

    // Securely checks whether the provided credentials belong to a moderator.
    // - Uses prepared statements to avoid SQL injection
    // - Retrieves password hash, salt, algorithm and iterations from DB (no hardcoded credentials)
    // - Verifies password using PBKDF2 in constant time
    // - Returns false on any error (fails closed)
    private boolean check_mod(String username, String password) {
        if (username == null || password == null) return false;

        // DB connection parameters must be provided via environment variables.
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            LOG.log(Level.WARNING, "Database credentials not configured in environment");
            return false;
        }

        String sql = "SELECT password_hash, password_salt, hash_algorithm, iterations, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false; // user not found
                }

                String storedHash = rs.getString("password_hash");
                String saltB64 = rs.getString("password_salt");
                String algo = rs.getString("hash_algorithm");
                int iterations = rs.getInt("iterations");
                String role = rs.getString("role");

                if (role == null || !"moderator".equalsIgnoreCase(role.trim())) {
                    return false;
                }

                if (storedHash == null || saltB64 == null) {
                    return false;
                }

                // Enforce sane iteration count
                if (iterations <= 0) iterations = MIN_PBKDF2_ITERATIONS;
                if (iterations < MIN_PBKDF2_ITERATIONS) iterations = MIN_PBKDF2_ITERATIONS;

                // Normalize algorithm; default to PBKDF2WithHmacSHA256
                String algoName = "PBKDF2WithHmacSHA256";
                if ("PBKDF2WithHmacSHA1".equalsIgnoreCase(algo)) {
                    algoName = "PBKDF2WithHmacSHA1";
                }

                byte[] salt;
                try {
                    salt = Base64.getDecoder().decode(saltB64);
                } catch (IllegalArgumentException ex) {
                    LOG.log(Level.WARNING, "Invalid salt encoding for user", ex);
                    return false;
                }

                byte[] derived;
                try {
                    derived = pbkdf2(password.toCharArray(), salt, iterations, 256, algoName);
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Password hashing failure", ex);
                    return false;
                }

                // Compare stored hash and derived hash in constant time.
                // Accept stored hash in either base64 or hex representation.
                String derivedB64 = Base64.getEncoder().encodeToString(derived);
                if (constantTimeEquals(storedHash, derivedB64)) {
                    return true;
                }
                // try hex fallback
                String derivedHex = toHex(derived);
                return constantTimeEquals(storedHash, derivedHex);
            }

        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "Database error during authentication", ex);
            return false;
        }
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits, String algorithm) throws Exception {
        // keyLengthBits e.g. 256
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
        return skf.generateSecret(spec).getEncoded();
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] ab = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ab.length != bb.length) {
            // still run through loop to not leak timing
            int result = 0;
            int len = Math.max(ab.length, bb.length);
            for (int i = 0; i < len; i++) {
                byte x = i < ab.length ? ab[i] : 0;
                byte y = i < bb.length ? bb[i] : 0;
                result |= x ^ y;
            }
            return false;
        }
        int res = 0;
        for (int i = 0; i < ab.length; i++) {
            res |= ab[i] ^ bb[i];
        }
        return res == 0;
    }

    private String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            int v = b & 0xFF;
            if (v < 16) sb.append('0');
            sb.append(Integer.toHexString(v));
        }
        return sb.toString();
    }
}
// ...existing code...
