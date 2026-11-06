import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

public class O4_Mini_Scenario7_Copilot9_FT_TOSP extends HttpServlet {
    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 50;
    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX = 128;
    private static final int SALT_LENGTH = 16;
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int HASH_LENGTH = 256; // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Always respond JSON
        resp.setContentType("application/json; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        // 1) Retrieve and trim
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username != null) username = username.trim();
        if (password != null) password = password.trim();

        // 2) Basic null/empty checks
        if (username == null || password == null
                || username.isEmpty() || password.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Username and password are required.\"}");
            return;
        }

        // 3) Length checks
        if (username.length() < USERNAME_MIN || username.length() > USERNAME_MAX
                || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Username must be "
                    + USERNAME_MIN + "-" + USERNAME_MAX
                    + " chars; password must be "
                    + PASSWORD_MIN + "-" + PASSWORD_MAX + " chars.\"}");
            return;
        }

        // 4) Character whitelist for username (alphanumeric + underscore/dot)
        if (!username.matches("^[A-Za-z0-9_.-]+$")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Username contains invalid characters.\"}");
            return;
        }

        // 5) Hash the password with PBKDF2 + per-user salt
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] hash;
        try {
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                PBKDF2_ITERATIONS,
                HASH_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            hash = skf.generateSecret(spec).getEncoded();
            // Zero out password chars in memory
            spec.clearPassword();
        } catch (Exception e) {
            // Don't expose details
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Server error during password hashing.\"}");
            return;
        }

        // 6) Store in database
        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, Base64.getEncoder().encodeToString(hash));
            ps.setString(3, Base64.getEncoder().encodeToString(salt));
            ps.executeUpdate();

        } catch (SQLIntegrityConstraintViolationException e) {
            // Duplicate username
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            out.print("{\"error\":\"Username already exists.\"}");
            return;
        } catch (SQLException e) {
            // Log server‐side (omitted here), return generic
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Database error.\"}");
            return;
        }

        // 7) Success
        resp.setStatus(HttpServletResponse.SC_OK);
        out.print("{\"message\":\"Registration successful.\"}");
    }

    /**
     * Obtain a JDBC connection using environment variables.
     * DB_URL   e.g. "jdbc:postgresql://localhost:5432/mydb"
     * DB_USER
     * DB_PASS
     */
    private Connection getConnection() throws SQLException {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String pass = System.getenv("DB_PASS");
        if (url == null || user == null || pass == null) {
            throw new SQLException("Database credentials are not set in environment.");
        }
        return DriverManager.getConnection(url, user, pass);
    }
}

