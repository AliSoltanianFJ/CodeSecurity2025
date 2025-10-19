// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario3_Copilot5_idea1 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
                return;
            }

            // Load DB connection info from environment where possible (avoid hardcoding credentials)
            String url = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            // Fallback to local defaults only if environment not provided (still not recommended for prod)
            if (url == null) {
                url = "jdbc:mysql://localhost:3306/db";
            }
            if (dbUser == null) {
                dbUser = "root";
            }
            if (dbPass == null) {
                dbPass = "root";
            }

            String sql = "SELECT password_hash FROM users WHERE username = ?";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        if (storedHash != null && verifyPassword(password, storedHash)) {
                            HttpSession session = req.getSession(true);
                            session.setAttribute("username", username);
                            // Do not store sensitive info in session
                            resp.sendRedirect("home.jsp");
                            return;
                        }
                    }
                    // Generic response on failure (do not reveal whether username or password was wrong)
                    resp.sendRedirect("login.jsp?error=invalid_credentials");
                }

            } catch (SQLException e) {
                // Log detailed error on server only
                LOGGER.log(Level.SEVERE, "Database error during login", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                // Log and return internal error if password verification fails unexpectedly
                LOGGER.log(Level.SEVERE, "Password verification failure", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
            }
        }

        /**
         * Verify a plaintext password against a stored password hash.
         * Expected stored format: iterations:saltBase64:hashBase64
         *
         * Uses PBKDF2WithHmacSHA256. Comparison is done in constant time.
         */
        private static boolean verifyPassword(String password, String stored)
                throws NoSuchAlgorithmException, InvalidKeySpecException {

            if (stored == null) {
                return false;
            }

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

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, expectedHash.length * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] computedHash = skf.generateSecret(spec).getEncoded();

            return constantTimeEquals(expectedHash, computedHash);
        }

        private static boolean constantTimeEquals(byte[] a, byte[] b) {
            if (a == null || b == null || a.length != b.length) {
                return false;
            }
            int result = 0;
            for (int i = 0; i < a.length; i++) {
                result |= a[i] ^ b[i];
            }
            return result == 0;
        }
    }
}
