// ...existing code...
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.Base64;
import java.util.Optional;

public class Scenario3_Copilot7_idea1 {

    public static class LoginServlet extends HttpServlet {
        private static final int DEFAULT_ITERATIONS = 65536;
        private static final int KEY_LENGTH = 256; // bits

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null || username.isBlank() || password.isBlank()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
                return;
            }

            // Read DB connection info from environment variables (safer than hard-coding)
            String dbUrl = Optional.ofNullable(System.getenv("DB_URL")).orElse("jdbc:mysql://localhost:3306/db");
            String dbUser = Optional.ofNullable(System.getenv("DB_USER")).orElse("root");
            String dbPass = Optional.ofNullable(System.getenv("DB_PASS")).orElse("");

            UserRecord user;
            try {
                user = fetchUserRecord(dbUrl, dbUser, dbPass, username);
            } catch (SQLException e) {
                // Log to server logs only, do not expose DB details to client
                log("Database error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
                return;
            }

            if (!user.found) {
                // Do not reveal whether username or password was incorrect
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                return;
            }

            boolean verified;
            try {
                verified = verifyPassword(password, user.salt, user.hash, user.iterations);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                log("Password verification error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
                return;
            }

            if (!verified) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                return;
            }

            // Successful login: create session (do not expose sensitive user data)
            HttpSession session = req.getSession(true);
            session.setAttribute("username", username);
            // configure session timeout as appropriate (seconds)
            session.setMaxInactiveInterval(30 * 60); // 30 minutes

            // Redirect or respond OK
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("Login successful");
        }

        private static class UserRecord {
            final boolean found;
            final byte[] hash;
            final byte[] salt;
            final int iterations;

            UserRecord(boolean found, byte[] hash, byte[] salt, int iterations) {
                this.found = found;
                this.hash = hash;
                this.salt = salt;
                this.iterations = iterations;
            }
        }

        private UserRecord fetchUserRecord(String url, String dbUser, String dbPass, String username) throws SQLException {
            String sql = "SELECT password_hash, salt, iterations FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return new UserRecord(false, null, null, 0);
                    }
                    // Expect password_hash and salt stored as base64 strings and iterations as int (nullable)
                    String hashB64 = rs.getString("password_hash");
                    String saltB64 = rs.getString("salt");
                    int iterations = rs.getInt("iterations");
                    if (rs.wasNull()) {
                        iterations = DEFAULT_ITERATIONS;
                    }
                    byte[] hash = hashB64 != null ? Base64.getDecoder().decode(hashB64) : null;
                    byte[] salt = saltB64 != null ? Base64.getDecoder().decode(saltB64) : null;
                    return new UserRecord(true, hash, salt, iterations);
                }
            }
        }

        private boolean verifyPassword(String password, byte[] salt, byte[] expectedHash, int iterations)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            if (salt == null || expectedHash == null) {
                return false;
            }
            byte[] computed = pbkdf2(password.toCharArray(), salt, iterations > 0 ? iterations : DEFAULT_ITERATIONS, KEY_LENGTH);
            return constantTimeEquals(computed, expectedHash);
        }

        private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        }

        private boolean constantTimeEquals(byte[] a, byte[] b) {
            if (a == null || b == null) return false;
            if (a.length != b.length) return false;
            int diff = 0;
            for (int i = 0; i < a.length; i++) {
                diff |= a[i] ^ b[i];
            }
            return diff == 0;
        }
    }
}
