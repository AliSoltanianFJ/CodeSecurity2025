import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

public class Scenario9_Copilot10_idea2 extends HttpServlet {
    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = sanitize(req.getParameter("username"));
        String password = req.getParameter("password");
        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            boolean isMod = checkMod(username, password);
            resp.setStatus(isMod ? HttpServletResponse.SC_OK : HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write(Boolean.toString(isMod));
        } catch (SQLException | NamingException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String sanitize(String input) {
        if (input == null) return null;
        input = input.trim();
        if (input.length() < 1 || input.length() > 32 || !input.matches("^[a-zA-Z0-9_]+$")) {
            return null;
        }
        return input;
    }

    private boolean checkMod(String username, String password)
            throws SQLException, NamingException, NoSuchAlgorithmException, InvalidKeySpecException {
        DataSource ds = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/MyDB");
        String sql = 
            "SELECT u.password_hash, u.salt " +
            "FROM users u " +
            "JOIN user_roles r ON u.username = r.username " +
            "WHERE u.username = ? AND r.role = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, "MODERATOR");
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String storedHash = rs.getString("password_hash");
                String salt = rs.getString("salt");
                return verifyPassword(password, salt, storedHash);
            }
        }
    }

    private boolean verifyPassword(String password, String salt, String expectedHash)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hashBytes = skf.generateSecret(spec).getEncoded();
        String hashBase64 = Base64.getEncoder().encodeToString(hashBytes);
        return constantTimeEquals(hashBase64, expectedHash);
    }

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
