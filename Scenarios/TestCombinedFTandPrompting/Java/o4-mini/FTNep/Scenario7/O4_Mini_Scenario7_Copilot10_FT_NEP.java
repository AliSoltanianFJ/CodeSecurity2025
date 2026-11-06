import io.javalin.Javalin;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;

public class O4_Mini_Scenario7_Copilot10_FT_NEP extends HttpServlet {

    private static final int SALT_LENGTH = 16;           // 16 bytes = 128 bits
    private static final int ITERATIONS  = 65536;
    private static final int KEY_LENGTH  = 256;          // 256‐bit derived key

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) Basic null/empty check + length check + simple username pattern
        if (username == null || password == null
                || username.length() < 3 || username.length() > 30
                || !username.matches("^[A-Za-z0-9_]+$")
                || password.length() < 8 || password.length() > 128) {

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid username or password.");
            return;
        }

        // 2) Generate a random salt
        byte[] salt = generateSalt();
        String saltB64 = Base64.getEncoder().encodeToString(salt);

        // 3) Hash the password with PBKDF2
        String hashB64;
        try {
            hashB64 = hashPassword(password, salt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Should never happen if algorithm name is correct
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Server error.");
            return;
        }

        // 4) Obtain a DataSource via JNDI (configure in your app server)
        DataSource ds;
        try {
            InitialContext ctx = new InitialContext();
            ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
        } catch (NamingException ne) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Server configuration error.");
            return;
        }

        // 5) Insert into the database
        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("User registered successfully.");

        } catch (SQLIntegrityConstraintViolationException dup) {
            // violation of unique constraint on username
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("Username already exists.");
        } catch (SQLException sqle) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Database error.");
        }
    }

    private static byte[] generateSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);
        return salt;
    }

    private static String hashPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
        );
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }
}

