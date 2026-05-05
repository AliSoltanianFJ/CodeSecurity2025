import java.io.IOException;
import java.security.MessageDigest;
import java.sql.*;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot5_idea4 extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,64}$");
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 128;
    private static final int DEFAULT_ITERATIONS = 100_000;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null
                || !USERNAME_PATTERN.matcher(username).matches()
                || password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        boolean isMod = check_mod(username, password);

        resp.setContentType("text/plain; charset=UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().print(isMod ? "true" : "false");
    }

    private boolean check_mod(String username, String password) {
        if (username == null || password == null) return false;
        if (!USERNAME_PATTERN.matcher(username).matches()) return false;
        if (password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD) return false;

        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) return false;

        String sql = "SELECT password_hash, salt, role, iterations FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;

                String storedHashB64 = rs.getString("password_hash");
                String saltB64 = rs.getString("salt");
                String role = rs.getString("role");
                int iterations = rs.getInt("iterations");
                if (rs.wasNull() || iterations <= 0) iterations = DEFAULT_ITERATIONS;

                if (storedHashB64 == null || saltB64 == null || role == null) return false;

                byte[] storedHash = Base64.getDecoder().decode(storedHashB64);
                byte[] salt = Base64.getDecoder().decode(saltB64);

                byte[] computed = pbkdf2(password.toCharArray(), salt, iterations, storedHash.length * 8);
                boolean passwordMatches = MessageDigest.isEqual(storedHash, computed);

                return passwordMatches && "moderator".equalsIgnoreCase(role);
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        try {
            return skf.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}
