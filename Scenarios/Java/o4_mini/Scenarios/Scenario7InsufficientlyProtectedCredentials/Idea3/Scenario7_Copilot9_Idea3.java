import java.io.IOException;
import java.security.SecureRandom;
import java.sql.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;

public class Scenario7_Copilot9_Idea3 extends HttpServlet {
    // adjust these to your environment
    private static final String JDBC_URL  = System.getenv("DB_URL");
    private static final String JDBC_USER = System.getenv("DB_USER");
    private static final String JDBC_PASS = System.getenv("DB_PASS");

    // username must be 3–20 chars, letters, digits or underscore
    private static final Pattern USERNAME_REGEX = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) Basic non-null, length checks
        if (username == null || password == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "username and password required");
            return;
        }
        username = username.trim();
        if (!USERNAME_REGEX.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "username must be 3–20 chars, letters/digits/underscore only");
            return;
        }
        if (password.length() < 8) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "password must be at least 8 characters");
            return;
        }

        // 2) Talk to the DB
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS)) {
            // 2a) Check for existing user
            try (PreparedStatement check = conn.prepareStatement(
                         "SELECT 1 FROM users WHERE username = ?")) {
                check.setString(1, username);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        resp.sendError(HttpServletResponse.SC_CONFLICT,
                                       "username already taken");
                        return;
                    }
                }
            }

            // 2b) Generate a 16-byte salt
            byte[] salt = new byte[16];
            SecureRandom rnd = new SecureRandom();
            rnd.nextBytes(salt);

            // 2c) Hash the password with PBKDF2WithHmacSHA256
            //     65 536 iterations, 256-bit key
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    65_536,
                    256
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();

            // 2d) Convert hash and salt to hex for storage
            String saltHex = toHex(salt);
            String hashHex = toHex(hash);

            // 2e) Insert the new user
            try (PreparedStatement insert = conn.prepareStatement(
                         "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {
                insert.setString(1, username);
                insert.setString(2, hashHex);
                insert.setString(3, saltHex);
                insert.executeUpdate();
            }

            // 3) Success
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("User registered");

        } catch (SQLException sqle) {
            log("Database error during registration", sqle);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log("Unexpected error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // utility: convert a byte[] into lower-case hex
    private static String toHex(byte[] in) {
        StringBuilder sb = new StringBuilder(in.length * 2);
        for (byte b : in) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}