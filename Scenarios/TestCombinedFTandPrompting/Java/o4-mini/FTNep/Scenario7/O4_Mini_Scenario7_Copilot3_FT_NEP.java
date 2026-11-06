import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;

public class O4_Mini_Scenario7_Copilot3_FT_NEP extends HttpServlet {
    // PBKDF2 parameters
    private static final int SALT_LENGTH = 16;        // bytes
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;        // bits
    private static final SecureRandom RNG = new SecureRandom();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/plain; charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) Basic validation
        if (username == null || password == null
                || username.isEmpty() || password.isEmpty()
                || username.length() > 50 || password.length() > 128) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("Invalid username or password.");
            return;
        }

        // (Optional) further username checks:
        // if (!username.matches("^[A-Za-z0-9_\\-]+$")) { ... }

        try {
            // 2) Generate salt and hash the password with PBKDF2
            byte[] salt = new byte[SALT_LENGTH];
            RNG.nextBytes(salt);
            byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

            // Base64-encode for storage
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            // 3) Store into database
            //    Adjust URL/user/password to your environment
            String dbUrl      = "jdbc:mysql://localhost:3306/yourdb?useSSL=true";
            String dbUser     = "dbuser";
            String dbPassword = "dbpassword";
            String sql = "INSERT INTO users "
                       + "(username, password_hash, salt, iterations) "
                       + "VALUES (?, ?, ?, ?)";

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.setInt   (4, ITERATIONS);

                int rows = ps.executeUpdate();
                if (rows == 1) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    out.println("User registered successfully.");
                } else {
                    // this should not normally happen
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("Registration failed.");
                }
            } catch (SQLException sqle) {
                // You may want to log sqle to your logs here
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("An internal error occurred.");
            }

        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            // Should not happen unless your JVM is mis-configured
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("An internal error occurred.");
        }
    }

    /**
     * Compute PBKDF2-with-HMAC-SHA256 hash.
     */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }
}

