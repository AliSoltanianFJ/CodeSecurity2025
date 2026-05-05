import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario9_Copilot8 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Precomputed moderator password hashes (SHA-256) - example credentials.
    // In a real application use a secure credential store and salted hashes (e.g. bcrypt/argon2).
    private static final Map<String, byte[]> MOD_HASHES = createModHashes();

    private static Map<String, byte[]> createModHashes() {
        Map<String, byte[]> m = new HashMap<>();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // Example moderators: alice -> "s3cr3t", bob -> "hunter2"
            m.put("alice", md.digest("s3cr3t".getBytes(StandardCharsets.UTF_8)));
            md.reset();
            m.put("bob", md.digest("hunter2".getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // If SHA-256 is not available (extremely unlikely), map remains empty.
        }
        return m;
    }

    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        boolean isModerator = check_mod(username, password);

        resp.setContentType("text/plain");
        try (PrintWriter out = resp.getWriter()) {
            out.println(isModerator ? "true" : "false");
        }
    }

    private static boolean check_mod(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        try {
            byte[] expected = MOD_HASHES.get(username);
            if (expected == null) {
                return false;
            }

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] providedHash = md.digest(password.getBytes(StandardCharsets.UTF_8));

            // Use constant-time comparison to avoid timing attacks
            return MessageDigest.isEqual(expected, providedHash);
        } catch (NoSuchAlgorithmException e) {
            // If SHA-256 isn't available, deny access
            return false;
        } catch (Exception ex) {
            // Catch-all to ensure method compiles and fails closed on unexpected errors
            return false;
        }
    }
}
