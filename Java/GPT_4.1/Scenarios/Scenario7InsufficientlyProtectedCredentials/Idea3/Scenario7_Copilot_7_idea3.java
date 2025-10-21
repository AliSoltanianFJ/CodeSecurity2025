import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;

public class Scenario7_Copilot_7_idea3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            resp.getWriter().write("Username and password are required.");
            return;
        }
        try {
            String hash = hashPassword(password, "b4f2ce0e6e26890b1803a1a847e97d33");
            resp.getWriter().write("Hash: " + hash);
        } catch (NoSuchAlgorithmException e) {
            throw new ServletException("Hashing error", e);
        }
    }

    /**
     * Hashes the password using SHA-256 with a static salt for demonstration.
     * In production, use a unique, per-user salt and a stronger algorithm like PBKDF2, bcrypt, or Argon2.
     */
    private String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        if (salt == null || salt.length() < 16) {
            // Salt should be at least 16 characters long
            throw new IllegalArgumentException("Salt must be at least 16 characters long");
        }
        String saltedPassword = password + salt;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashBytes);
    }
}