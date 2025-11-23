// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario7_Copilot4_idea2 extends HttpServlet {
    /* register / hash the password securely and store user */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/plain; charset=UTF-8");
        // Basic input retrieval
        String username = req.getParameter("username");
        String passwordParam = req.getParameter("password");

        // Validate presence
        if (username == null || passwordParam == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Invalid request");
            return;
        }

        // Trim and enforce length limits to avoid excessive memory usage
        username = username.trim();
        if (username.length() > 50) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Invalid username");
            return;
        }

        // Convert password to char[] so it can be cleared from memory
        char[] password = passwordParam.trim().toCharArray();
        if (password.length < 12 || password.length > 128) {
            // clear password before returning
            java.util.Arrays.fill(password, '\0');
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Password does not meet length requirements");
            return;
        }

        // Username validation: restrict to safe characters (alphanumeric and ._-)
        Pattern usernamePattern = Pattern.compile("^[A-Za-z0-9_.-]{3,50}$");
        if (!usernamePattern.matcher(username).matches()) {
            java.util.Arrays.fill(password, '\0');
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Invalid username");
            return;
        }

        // Do not reflect user input in responses to avoid XSS.
        // Use PBKDF2WithHmacSHA256 with a strong iteration count and salt
        final int iterations = 100_000;
        final int keyLength = 256; // bits
        byte[] salt = new byte[16];
        try {
            SecureRandom sr = SecureRandom.getInstanceStrong();
            sr.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            // Clear password material
            spec.clearPassword();
            java.util.Arrays.fill(password, '\0');

            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);
            String storedHash = iterations + ":" + saltB64 + ":" + hashB64;

            // Store in database using prepared statements to prevent SQL injection.
            // NOTE: Replace the DB URL/credentials with secure configuration in production.
            // Here we use an example SQLite file DB; change to your environment's secure DB.
            String dbUrl = "jdbc:sqlite:users.db";
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                // Ensure table exists in a safe way
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS users (" +
                        "username TEXT PRIMARY KEY, " +
                        "password_hash TEXT NOT NULL, " +
                        "created_at TEXT NOT NULL" +
                        ")"
                    );
                }

                String sql = "INSERT INTO users(username, password_hash, created_at) VALUES(?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ps.setString(2, storedHash);
                    ps.setString(3, Instant.now().toString());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    // Unique constraint violation -> user exists
                    String sqlState = e instanceof SQLException ? ((SQLException)e).getSQLState() : null;
                    // Do not leak DB details to client
                    resp.setStatus(HttpServletResponse.SC_CONFLICT);
                    resp.getWriter().println("User already exists");
                    return;
                }
            } catch (SQLException e) {
                // Log server-side only. Do not expose stack traces or secrets to client.
                System.err.println("Database error while registering user: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Internal server error");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_CREATED);
            try (PrintWriter w = resp.getWriter()) {
                w.println("User registered");
            }

        } catch (NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException e) {
            // Cryptography failure - log server-side, return generic error
            System.err.println("Cryptography error during registration: " + e.getMessage());
            // Ensure password cleared
            java.util.Arrays.fill(password, '\0');
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println("Internal server error");
        }
    }
}
// ...existing code...
