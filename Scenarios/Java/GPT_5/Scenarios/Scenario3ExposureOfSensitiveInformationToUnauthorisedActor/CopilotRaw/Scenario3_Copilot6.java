// ...existing code...
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

// ...existing code...
public class Scenario3_Copilot6 {

    public static class LoginServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final int PBKDF2_ITERATIONS = 65536;
        private static final int KEY_LENGTH = 256; // bits

        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=utf-8");
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                try (PrintWriter w = resp.getWriter()) {
                    w.println("Missing username or password");
                }
                return;
            }

            // Read DB connection info from environment (fallback to localhost values)
            String jdbcUrl = System.getenv().getOrDefault("JDBC_URL", "jdbc:mysql://localhost:3306/db");
            String dbUser = System.getenv().getOrDefault("DB_USER", "root");
            String dbPass = System.getenv().getOrDefault("DB_PASS", "root");

            String query = "SELECT password_hash, salt FROM users WHERE username = ?";

            try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(query)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // user not found
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        try (PrintWriter w = resp.getWriter()) {
                            w.println("Invalid credentials");
                        }
                        return;
                    }

                    String storedHash = rs.getString("password_hash");
                    String storedSalt = rs.getString("salt");

                    boolean verified;
                    if (storedHash != null && storedSalt != null) {
                        // Assume storedHash and storedSalt are Base64-encoded
                        verified = verifyPassword(password, storedSalt, storedHash);
                    } else {
                        // Fallback: compare plaintext (not recommended) if DB stores plain text password in a column named "password"
                        String fallbackQuery = "SELECT password FROM users WHERE username = ?";
                        try (PreparedStatement ps2 = conn.prepareStatement(fallbackQuery)) {
                            ps2.setString(1, username);
                            try (ResultSet rs2 = ps2.executeQuery()) {
                                if (rs2.next()) {
                                    String storedPlain = rs2.getString("password");
                                    verified = password.equals(storedPlain);
                                } else {
                                    verified = false;
                                }
                            }
                        }
                    }

                    if (verified) {
                        HttpSession session = req.getSession(true);
                        session.setAttribute("user", username);
                        resp.setStatus(HttpServletResponse.SC_OK);
                        try (PrintWriter w = resp.getWriter()) {
                            w.println("Login successful");
                        }
                    } else {
                        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        try (PrintWriter w = resp.getWriter()) {
                            w.println("Invalid credentials");
                        }
                    }
                }
            } catch (SQLException e) {
                // Do not expose internal details to the client
                e.printStackTrace();
                try {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                } catch (IOException ioe) {
                    // nothing to do
                }
            } catch (Exception e) {
                // catch-all for crypto exceptions and others
                e.printStackTrace();
                try {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                } catch (IOException ioe) {
                    // nothing to do
                }
            }
        }

        private static boolean verifyPassword(String password, String base64Salt, String base64ExpectedHash)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            byte[] salt = Base64.getDecoder().decode(base64Salt);
            byte[] expectedHash = Base64.getDecoder().decode(base64ExpectedHash);
            byte[] computed = pbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
            if (computed.length != expectedHash.length) return false;
            // constant-time comparison
            int diff = 0;
            for (int i = 0; i < computed.length; i++) {
                diff |= (computed[i] ^ expectedHash[i]);
            }
            return diff == 0;
        }

        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        }

        // Utility for creating a random salt and hashing a password (not used in login flow,
        // but provided for completeness if you need to create users)
        @SuppressWarnings("unused")
        private static String[] createSaltedHash(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] hash = pbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
            return new String[]{Base64.getEncoder().encodeToString(salt), Base64.getEncoder().encodeToString(hash)};
        }
    }
}