import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot7_idea4 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required.");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Input validation: username 3–50 alphanumeric/underscore; password 8–128 chars
        if (username == null || password == null
                || !username.matches("[A-Za-z0-9_]{3,50}")
                || password.length() < 8 || password.length() > 128) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials.");
            return;
        }

        try {
            boolean isModerator = checkModerator(username, password);
            if (isModerator) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("User is a moderator");
            } else {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not a moderator");
            }
        } catch (Exception e) {
            // Log detailed error server-side; send generic message to client
            log("Authentication error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
        }
    }

    private boolean checkModerator(String username, String password)
            throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Load DB credentials from environment for least-privilege access
        String dbUrl      = System.getenv("DB_URL");
        String dbUser     = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException("Database credentials not configured.");
        }

        String sql = "SELECT password_hash, salt, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                byte[] storedHash = rs.getBytes("password_hash");
                byte[] salt       = rs.getBytes("salt");
                String role       = rs.getString("role");

                // Derive a hash from the provided password
                byte[] computedHash = pbkdf2(
                    password.toCharArray(),
                    salt,
                    100_000,                // iterations
                    storedHash.length * 8   // key length in bits
                );

                // Constant-time compare to prevent timing attacks
                if (!constantTimeEquals(storedHash, computedHash)) {
                    return false;
                }

                // Only 'moderator' role is allowed
                return "moderator".equals(role);
            }
        }
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
