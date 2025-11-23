// ...existing code...
import io.javalin.Javalin;
import java.security.SecureRandom;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Scenario7_Copilot8_idea4 extends HttpServlet {
    private static final SecureRandom RNG = new SecureRandom();
    private static final int SALT_LEN = 16;
    private static final int ITERATIONS = 200_000;
    private static final int KEY_LENGTH = 256;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
    private static final int PASSWORD_MIN = 12;
    private static final int PASSWORD_MAX = 128;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().write("Missing required parameters.");
            return;
        }

        username = username.trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().write("Invalid username.");
            return;
        }

        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().write("Invalid password.");
            return;
        }

        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().write("Server configuration error.");
            return;
        }

        byte[] salt = new byte[SALT_LEN];
        RNG.nextBytes(salt);

        byte[] hash;
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            hash = skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().write("Server error.");
            return;
        }

        String b64Salt = Base64.getEncoder().encodeToString(salt);
        String b64Hash = Base64.getEncoder().encodeToString(hash);

        String insertSql = "INSERT INTO users (username, password_hash, salt, iterations) VALUES (?, ?, ?, ?)";
        String checkSql = "SELECT COUNT(1) FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                psCheck.setString(1, username);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        conn.rollback();
                        resp.setStatus(HttpServletResponse.SC_CONFLICT);
                        resp.setContentType("text/plain; charset=utf-8");
                        resp.getWriter().write("User already exists.");
                        return;
                    }
                }
            }

            try (PreparedStatement psIns = conn.prepareStatement(insertSql)) {
                psIns.setString(1, username);
                psIns.setString(2, b64Hash);
                psIns.setString(3, b64Salt);
                psIns.setInt(4, ITERATIONS);
                psIns.executeUpdate();
            }

            conn.commit();
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().write("Registered.");
        } catch (SQLException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain; charset=utf-8");
            resp.getWriter().write("Server error.");
        }
    }
}
// ...existing code...
