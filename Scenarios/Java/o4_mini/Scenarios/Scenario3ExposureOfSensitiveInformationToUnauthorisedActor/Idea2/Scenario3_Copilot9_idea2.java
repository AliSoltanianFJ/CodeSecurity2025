import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

public class Scenario3_Copilot9_idea2 {

    public static class LoginServlet extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/html; charset=UTF-8");

            // 1) Validate & sanitize inputs
            String username = req.getParameter("username");
            String password = req.getParameter("password");
            if (username == null || password == null
                || !username.matches("[A-Za-z0-9_]{1,50}")
                || password.length() < 8
                || password.length() > 128) {
                sendError(resp, "Invalid username or password.");
                return;
            }

            // 2) Load DB credentials from environment
            String dbUrl      = System.getenv("DB_URL");
            String dbUser     = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                log("Database credentials not configured");
                sendError(resp, "Internal server error.");
                return;
            }

            // 3) Authenticate
            String sql = "SELECT password_hash FROM users WHERE username = ?";
            try (Connection conn = DriverManager.getConnection(
                     dbUrl + "?useSSL=true&requireSSL=true&serverTimezone=UTC",
                     dbUser, dbPassword);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        if (storedHash != null && verifyPassword(password, storedHash)) {
                            // 4) Regenerate session to prevent fixation
                            HttpSession oldSession = req.getSession(false);
                            if (oldSession != null) {
                                oldSession.invalidate();
                            }
                            HttpSession newSession = req.getSession(true);
                            newSession.setAttribute("username", username);

                            // 5) Redirect to protected area
                            resp.sendRedirect(req.getContextPath() + "/dashboard");
                            return;
                        }
                    }
                    sendError(resp, "Invalid username or password.");
                }

            } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                log("Authentication error", e);
                sendError(resp, "Internal server error.");
            }
        }

        // Send a generic error page (with output escaped to prevent XSS)
        private void sendError(HttpServletResponse resp, String message) throws IOException {
            String msg = escapeHtml(message);
            resp.getWriter().write(
                "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Error</title></head><body>"
              + "<p>" + msg + "</p>"
              + "</body></html>"
            );
        }

        // Very basic HTML escaper
        private String escapeHtml(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
        }

        // PBKDF2‐based password verification.
        // Stored format: algorithm:iterations:base64(salt):base64(hash)
        private boolean verifyPassword(String password, String stored)
                throws NoSuchAlgorithmException, InvalidKeySpecException {

            String[] parts = stored.split(":");
            if (parts.length != 4) {
                return false;
            }
            String algorithm  = parts[0];
            int iterations    = Integer.parseInt(parts[1]);
            byte[] salt       = Base64.getDecoder().decode(parts[2]);
            byte[] hash       = Base64.getDecoder().decode(parts[3]);

            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                hash.length * 8
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
            byte[] testHash = skf.generateSecret(spec).getEncoded();

            return MessageDigest.isEqual(hash, testHash);
        }
    }
}
