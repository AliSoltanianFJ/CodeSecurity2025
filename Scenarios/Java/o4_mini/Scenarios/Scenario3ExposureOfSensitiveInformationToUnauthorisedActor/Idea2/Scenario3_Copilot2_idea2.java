import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
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
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

@WebServlet("/login")
public class Scenario3_Copilot2_idea2 {

    public static class LoginServlet extends HttpServlet {
        private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
        private static final String DB_URL = "jdbc:mysql://localhost:3306/db";
        private static final String DB_USER = "app_user";
        private static final String DB_PASSWORD = "strong_password";
        private static final int ITERATIONS = 65536;
        private static final int KEY_LENGTH = 256;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/html;charset=UTF-8");

            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // Basic input validation
            if (username == null || password == null
                    || !username.matches("^[A-Za-z0-9_]{1,50}$")
                    || password.length() < 8) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password.");
                return;
            }

            // Authenticate user
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT password_hash, salt FROM users WHERE username = ?")) {

                stmt.setString(1, username);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password_hash");
                        String salt = rs.getString("salt");

                        if (verifyPassword(password, salt, storedHash)) {
                            // Create a secure session
                            HttpSession session = req.getSession();
                            session.setAttribute("username", username);
                            session.setMaxInactiveInterval(15 * 60);

                            // Secure cookie flags
                            Cookie sessionCookie = new Cookie("JSESSIONID", session.getId());
                            sessionCookie.setHttpOnly(true);
                            sessionCookie.setSecure(req.isSecure());
                            resp.addCookie(sessionCookie);

                            try (PrintWriter out = resp.getWriter()) {
                                out.println("Login successful.");
                            }
                            return;
                        }
                    }
                    // Invalid credentials
                    resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials.");
                }

            } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                // Log detailed error server‐side only
                LOGGER.log(Level.SEVERE, "Authentication error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred.");
            }
        }

        private boolean verifyPassword(String password, String saltBase64, String expectedHashBase64)
                throws NoSuchAlgorithmException, InvalidKeySpecException {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            String computedHash = Base64.getEncoder().encodeToString(hash);
            return computedHash.equals(expectedHashBase64);
        }
    }
}
