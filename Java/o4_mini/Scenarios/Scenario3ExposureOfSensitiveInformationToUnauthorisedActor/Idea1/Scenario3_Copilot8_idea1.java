package Scenarios.Scenario3ExposureOfSensitiveInformationToUnauthorisedActor;

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
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario3_Copilot8_idea1 {

    public static class LoginServlet extends HttpServlet {
        /* log in the user if the username and password are correct */
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username and password are required.");
                return;
            }

            // Read DB connection information from environment to avoid hardcoding secrets in source.
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            if (dbUrl == null || dbUser == null || dbPass == null) {
                log("Missing database configuration in environment variables (DB_URL/DB_USER/DB_PASS).");
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error.");
                return;
            }

            String sql = "SELECT id, password_hash, salt, iterations FROM users WHERE username = ?";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // Do not reveal whether username or password was incorrect
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                        return;
                    }

                    int userId = rs.getInt("id");
                    String storedHashB64 = rs.getString("password_hash");
                    String saltB64 = rs.getString("salt");
                    int iterations = rs.getInt("iterations");

                    boolean ok;
                    try {
                        ok = verifyPassword(password, storedHashB64, saltB64, iterations);
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
                        // log details server-side, do not expose to client
                        log("Password verification error for user " + username, e);
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error.");
                        return;
                    }

                    if (ok) {
                        HttpSession session = req.getSession(true);
                        // Store non-sensitive identifier in session
                        session.setAttribute("userId", userId);
                        session.setAttribute("username", username);
                        resp.sendRedirect(req.getContextPath() + "/home.jsp");
                    } else {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                    }
                }

            } catch (SQLException e) {
                // Log details to server logs; do not expose DB internals to the client
                log("Database error during login for user: " + username, e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
            }
        }

        /**
         * Verify a password against a stored PBKDF2 hash (Base64), salt (Base64) and iteration count.
         * Uses PBKDF2WithHmacSHA256 and a constant-time comparison.
         */
        private boolean verifyPassword(String password, String storedHashB64, String saltB64, int iterations)
                throws NoSuchAlgorithmException, InvalidKeySpecException {

            if (password == null || storedHashB64 == null || saltB64 == null || iterations <= 0) {
                throw new IllegalArgumentException("Invalid arguments to verifyPassword");
            }

            byte[] storedHash = Base64.getDecoder().decode(storedHashB64);
            byte[] salt = Base64.getDecoder().decode(saltB64);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, storedHash.length * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] computedHash = skf.generateSecret(spec).getEncoded();

            // Constant-time comparison to reduce timing attack risk
            return MessageDigest.isEqual(storedHash, computedHash);
        }
    }
}
