// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario3_Copilot4_idea1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
                // Generic error redirect for missing credentials
                resp.sendRedirect("login.jsp?error=missing_credentials");
                return;
            }

            // Read DB connection info from environment where possible (do not hardcode credentials in source)
            String url = System.getenv().getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/db");
            String dbUser = System.getenv().getOrDefault("DB_USER", "app_user");
            String dbPass = System.getenv().getOrDefault("DB_PASS", "change_me");

            String sql = "SELECT password_hash FROM users WHERE username = ?";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String stored = rs.getString("password_hash");
                        boolean ok = false;
                        try {
                            ok = verifyPassword(stored, password);
                        } catch (GeneralSecurityException ex) {
                            // treat verification errors as authentication failure, log internally
                            log("Password verification error for user: " + username, ex);
                        }

                        if (ok) {
                            // Prevent session fixation: create / rotate session id
                            HttpSession session = req.getSession(true);
                            try {
                                // Servlet 3.1+ method to change session id (no-op on older containers)
                                req.changeSessionId();
                            } catch (NoSuchMethodError | AbstractMethodError ignored) {
                                // ignore if container doesn't support changeSessionId()
                            }
                            session.setAttribute("username", username);
                            // Do not store sensitive info in the session
                            resp.sendRedirect("home.jsp");
                            return;
                        }
                    }

                    // Generic authentication failure (do not reveal whether username or password was wrong)
                    resp.sendRedirect("login.jsp?error=invalid_credentials");
                    return;
                }

            } catch (SQLException e) {
                // Log server-side only. Do not expose DB details to client.
                log("Database error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
            }
        }

        /**
         * Verify a provided password against a stored password representation.
         *
         * Expected stored formats supported here:
         *  - PBKDF2 format: iterations:saltBase64:hashBase64
         *
         * Returns false for any unknown format or verification errors.
         */
        private static boolean verifyPassword(String stored, String providedPassword) throws GeneralSecurityException {
            if (stored == null || providedPassword == null) {
                return false;
            }

            // PBKDF2 format: iterations:salt:hash (all base64 for salt/hash)
            if (stored.chars().filter(ch -> ch == ':').count() == 2) {
                String[] parts = stored.split(":");
                if (parts.length != 3) {
                    return false;
                }
                int iterations;
                try {
                    iterations = Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                    return false;
                }
                byte[] salt = Base64.getDecoder().decode(parts[1]);
                byte[] expectedHash = Base64.getDecoder().decode(parts[2]);

                byte[] pwdHash = pbkdf2(providedPassword.toCharArray(), salt, iterations, expectedHash.length * 8);
                return slowEquals(expectedHash, pwdHash);
            }

            // Unknown/unsupported format
            return false;
        }

        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) throws GeneralSecurityException {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        }

        private static boolean slowEquals(byte[] a, byte[] b) {
            return MessageDigest.isEqual(a, b);
        }
    }
}
// ...existing code...
