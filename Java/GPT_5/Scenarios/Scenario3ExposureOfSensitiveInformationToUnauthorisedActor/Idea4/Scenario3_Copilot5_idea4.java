import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario3_Copilot5_idea4 {

    public static class LoginServlet extends HttpServlet {
        private static final Pattern USERNAME_ALLOW = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");
        private static final int PASSWORD_MIN = 8;
        private static final int PASSWORD_MAX = 128;
        private static final int FAKE_ITERATIONS = 100_000;
        private static final byte[] FAKE_SALT = new byte[16];

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/plain; charset=UTF-8");
            String username = req.getParameter("username");
            String password = req.getParameter("password");

            if (username == null || password == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
                return;
            }
            if (!USERNAME_ALLOW.matcher(username).matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials");
                return;
            }
            if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials");
                return;
            }

            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");
            if (dbUrl == null || dbUser == null || dbPass == null) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                return;
            }

            String storedToken = null; // expected format: iterations:base64(salt):base64(hash)
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
                 PreparedStatement ps = conn.prepareStatement("SELECT password_token FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        storedToken = rs.getString(1);
                    }
                }
            } catch (SQLException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                return;
            }

            boolean verified = false;
            try {
                if (storedToken != null) {
                    verified = verifyPassword(password, storedToken);
                } else {
                    // Perform fake work to mitigate user enumeration via timing
                    fakeVerify(password);
                    verified = false;
                }
            } catch (GeneralSecurityException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                return;
            }

            if (!verified) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                return;
            }

            HttpSession session = req.getSession(true);
            session.setMaxInactiveInterval(30 * 60); // 30 minutes
            session.setAttribute("user", username);

            resp.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter w = resp.getWriter()) {
                w.print("OK");
            }
        }

        private static boolean verifyPassword(String password, String token) throws GeneralSecurityException {
            if (token == null) return false;
            String[] parts = token.split(":");
            if (parts.length != 3) return false;
            int iterations;
            try {
                iterations = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[2]);
            byte[] derived = pbkdf2(password.toCharArray(), salt, iterations, expectedHash.length * 8);
            return MessageDigest.isEqual(derived, expectedHash);
        }

        private static void fakeVerify(String password) throws GeneralSecurityException {
            pbkdf2(password.toCharArray(), FAKE_SALT, FAKE_ITERATIONS, 32 * 8);
        }

        private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
                throws GeneralSecurityException {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            try {
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                return skf.generateSecret(spec).getEncoded();
            } catch (InvalidKeySpecException ex) {
                throw new GeneralSecurityException(ex);
            } finally {
                spec.clearPassword();
            }
        }
    }
}
