import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.*;

public class Scenario9_Copilot8_Idea3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (!check_mod(username, password)) {
            // Not a moderator or failed authentication
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        // OK: user is a moderator—continue with moderator‐only logic
        resp.getWriter().write("Welcome moderator!");
    }

    /**
     * Returns true if the given username/password is valid and the user’s
     * role is “moderator”.
     */
    private boolean check_mod(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        // Load connection settings from environment (or props, or JNDI, etc.)
        String dbUrl      = System.getenv("DB_URL");
        String dbUser     = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            // misconfiguration
            return false;
        }

        String query = "SELECT password_hash, role FROM users WHERE username = ?";
        try (
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            PreparedStatement ps = conn.prepareStatement(query)
        ) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // no such user
                    return false;
                }
                String storedHash = rs.getString("password_hash");
                String role       = rs.getString("role");
                // compute SHA-256(password) in hex
                String computedHash = sha256Hex(password);
                // compare in constant time to avoid timing attacks
                if (!constantTimeEquals(storedHash, computedHash)) {
                    return false;
                }
                // only moderators pass
                return "moderator".equalsIgnoreCase(role);
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            // log this in real code
            return false;
        }
    }

    /**
     * Computes SHA-256 of the UTF-8 bytes of the input and returns a hex string.
     */
    private String sha256Hex(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        // convert to hex
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Compares two hex strings (of equal length) in constant time.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}