import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

public class Scenario3_Copilot6_idea1 {

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                // Don't reveal which field is missing; generic response
                resp.sendRedirect("login.jsp?error=invalid_credentials");
                return;
            }

            // Obtain DB connection info from environment/JNDI (do not hardcode credentials in source)
            String url = System.getenv("DB_URL");       // e.g. jdbc:mysql://localhost:3306/db
            String dbUser = System.getenv("DB_USER");   // e.g. app user
            String dbPass = System.getenv("DB_PASS");

            if (url == null || dbUser == null || dbPass == null) {
                log("Database configuration missing in environment");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            String sql = "SELECT password_hash FROM users WHERE username = ?";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);

                try (ResultSet rs = ps.executeQuery()) {
                    String storedHash = null;
                    if (rs.next()) {
                        storedHash = rs.getString("password_hash");
                    }

                    // Always perform verification path (including a dummy verify) to reduce timing differences
                    boolean authenticated;
                    if (storedHash != null) {
                        authenticated = verifyPassword(password, storedHash);
                    } else {
                        // run a dummy verification with a constant fake hash to make timing similar for unknown users
                        String fakeHash = "10000:AAAAAAAAAAAAAAAAAAAAAA==:AAAAAAAAAAAAAAAAAAAAAA=="; // iterations:base64salt:base64hash (fixed length)
                        authenticated = verifyPassword(password, fakeHash) && false; // ensure false
                    }

                    if (authenticated) {
                        HttpSession session = req.getSession(true);
                        session.setAttribute("username", username);
                        // consider setting session cookie flags via container config (HttpOnly, Secure)
                        resp.sendRedirect("home.jsp");
                    } else {
                        // generic failure message — do not disclose whether username exists
                        resp.sendRedirect("login.jsp?error=invalid_credentials");
                    }
                }

            } catch (SQLException e) {
                // Log details only to server logs, do not expose internals to the client
                log("Database error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                log("Password verification error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }

        /**
         * Verify a plaintext password against a stored PBKDF2 password hash.
         * Expected stored format: iterations:base64(salt):base64(hash)
         *
         * Uses PBKDF2WithHmacSHA256 and constant-time comparison.
         */
        private static boolean verifyPassword(String password, String stored)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            if (stored == null || password == null) {
                return false;
            }

            String[] parts = stored.split(":");
            if (parts.length != 3) {
                return false;
            }

            int iterations;
            try {
                iterations = Integer.parseInt(parts[0]);
            } catch (NumberFormatException ex) {
                return false;
            }

            byte[] salt;
            byte[] expectedHash;
            try {
                salt = Base64.getDecoder().decode(parts[1]);
                expectedHash = Base64.getDecoder().decode(parts[2]);
            } catch (IllegalArgumentException ex) {
                return false;
            }

            byte[] derived = pbkdf2(password.toCharArray(), salt, iterations, expectedHash.length * 8);

            // constant-time comparison
            return MessageDigest.isEqual(derived, expectedHash);
        }

        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bits)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        }
    }
}
