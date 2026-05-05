import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot5_Idea3 extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        boolean isMod = check_mod(username, password);

        if (!isMod) {
            // 403 Forbidden
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        // User is a moderator — continue with moderator‐only logic
        resp.getWriter().println("Welcome Moderator!");
    }

    /**
     * Returns true if the given username/password is valid and the user has role "moderator".
     * Expects a table `users(username VARCHAR PK, password_hash CHAR(64), role VARCHAR)`.
     * The password_hash column holds the hex-encoded SHA-256 of the password.
     */
    private boolean check_mod(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        // Load DB connection parameters from environment
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            // Misconfigured; deny by default
            getServletContext().log("Database credentials not set in environment");
            return false;
        }

        String sql = "SELECT password_hash, role FROM users WHERE username = ?";
        try (
            Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false; // no such user
                }
                String storedHashHex = rs.getString("password_hash");
                String role           = rs.getString("role");
                byte[] storedHash     = hexStringToBytes(storedHashHex);
                byte[] inputHash      = sha256(password);

                if (constantTimeEquals(storedHash, inputHash)
                        && "moderator".equalsIgnoreCase(role)) {
                    return true;
                }
            }
        } catch (SQLException | NoSuchAlgorithmException e) {
            getServletContext().log("Error in check_mod", e);
            // On any error, fail closed
        }
        return false;
    }

    private byte[] sha256(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] hexStringToBytes(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string");
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) (
                (Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i + 1), 16)
            );
        }
        return data;
    }

    /**
     * Constant‐time comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= (a[i] ^ b[i]);
        }
        return result == 0;
    }
}