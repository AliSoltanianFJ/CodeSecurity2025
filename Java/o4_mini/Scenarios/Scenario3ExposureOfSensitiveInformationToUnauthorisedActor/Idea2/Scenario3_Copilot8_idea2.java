import javax.servlet.ServletException;
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
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.regex.Pattern;

public class Scenario3_Copilot8_idea2 {

    public static class LoginServlet extends HttpServlet {
        private static final int MAX_INPUT_LENGTH = 50;
        private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");
        // Database credentials should be set as environment variables
        private static final String DB_URL  = System.getenv("DB_URL");
        private static final String DB_USER = System.getenv("DB_USER");
        private static final String DB_PASS = System.getenv("DB_PASS");

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // 1) Basic null/length/pattern validation
            if (username == null || password == null
                    || username.length() > MAX_INPUT_LENGTH
                    || password.length() > MAX_INPUT_LENGTH
                    || !USERNAME_PATTERN.matcher(username).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid input");
                return;
            }

            // 2) Lookup stored hash and salt using a prepared statement
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT password_hash, salt FROM users WHERE username = ?")) {

                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        return;
                    }

                    // Decode Base64‐encoded salt & hash
                    byte[] salt       = Base64.getDecoder().decode(rs.getString("salt"));
                    byte[] storedHash = Base64.getDecoder().decode(rs.getString("password_hash"));

                    // 3) Verify
                    if (!validatePassword(password, salt, storedHash)) {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                        return;
                    }

                    // 4) On success: create session & redirect
                    HttpSession session = req.getSession(true);
                    session.setAttribute("username", username);
                    resp.sendRedirect(req.getContextPath() + "/home");
                }

            } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                // log on server side only
                e.printStackTrace();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
            }
        }

        /**
         * Recomputes PBKDF2WithHmacSHA256(password, salt) and
         * compares in constant time to avoid timing attacks.
         */
        private boolean validatePassword(String password, byte[] salt, byte[] expectedHash)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                10000,
                expectedHash.length * 8
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] computed = skf.generateSecret(spec).getEncoded();
            return MessageDigest.isEqual(computed, expectedHash);
        }
    }
}
