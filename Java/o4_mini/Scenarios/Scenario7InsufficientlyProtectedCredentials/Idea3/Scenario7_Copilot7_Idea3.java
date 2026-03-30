import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

public class Scenario7_Copilot7_Idea3 extends HttpServlet {

    // PBKDF2 parameters
    private static final String  PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int     SALT_LENGTH      = 16;       // bytes
    private static final int     HASH_LENGTH      = 256;      // bits
    private static final int     ITERATIONS       = 100_000;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // parse & validate
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || username.trim().isEmpty()
         || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "username and password must be provided");
            return;
        }

        // delegate
        register(username.trim(), password, resp);
    }

    private void register(String username,
                          String password,
                          HttpServletResponse resp)
            throws IOException {

        // read JDBC config from environment
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Database configuration not set");
            return;
        }

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {

            // 1) check if username exists
            String checkSql = "SELECT 1 FROM users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        resp.setStatus(HttpServletResponse.SC_CONFLICT);
                        try (PrintWriter w = resp.getWriter()) {
                            w.print("{\"error\":\"Username already exists\"}");
                        }
                        return;
                    }
                }
            }

            // 2) generate random salt
            byte[] salt = new byte[SALT_LENGTH];
            SecureRandom rng = new SecureRandom();
            rng.nextBytes(salt);
            String saltB64 = Base64.getEncoder().encodeToString(salt);

            // 3) hash password with PBKDF2
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(), salt, ITERATIONS, HASH_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            // 4) insert new user
            String insertSql = "INSERT INTO users (username, salt, password_hash) VALUES (?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, username);
                ps.setString(2, saltB64);
                ps.setString(3, hashB64);
                ps.executeUpdate();
            }

            // 5) success
            resp.setStatus(HttpServletResponse.SC_CREATED);
            try (PrintWriter w = resp.getWriter()) {
                w.print("{\"message\":\"User registered\"}");
            }

        } catch (SQLIntegrityConstraintViolationException dup) {
            // in case the UNIQUE constraint fired anyway
            resp.sendError(HttpServletResponse.SC_CONFLICT,
                           "Username already exists");
        } catch (Exception e) {
            // log on the server side
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Error during registration");
        }
    }
}