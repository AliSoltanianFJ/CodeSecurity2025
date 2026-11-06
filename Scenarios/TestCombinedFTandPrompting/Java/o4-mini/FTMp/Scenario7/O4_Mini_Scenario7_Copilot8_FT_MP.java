import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

public class O4_Mini_Scenario7_Copilot8_FT_MP extends HttpServlet {
    // Enforce a tight username policy: 3–30 chars, alphanumeric + underscore only.
    private static final Pattern USERNAME_PATTERN =
        Pattern.compile("^[A-Za-z0-9_]{3,30}$");

    // PBKDF2 parameters: high iteration count, 256-bit derived key
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int    SALT_LENGTH       = 16;       // bytes
    private static final int    ITERATIONS        = 100_000;
    private static final int    KEY_LENGTH        = 256;      // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Require UTF-8 for inbound parameters to prevent encoding tricks
        req.setCharacterEncoding("UTF-8");
        // Always respond in JSON
        resp.setContentType("application/json; charset=UTF-8");

        // Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                "{\"error\":\"Secure connection (HTTPS) required.\"}");
            return;
        }

        // Extract and trim inputs
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username != null)  username = username.trim();
        if (password != null)  password = password.trim();

        // Basic null/empty check
        if (username == null || username.isEmpty() ||
            password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "{\"error\":\"Username and password must be provided.\"}");
            return;
        }

        // Validate username against policy
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "{\"error\":\"Invalid username format.\"}");
            return;
        }

        // Enforce password length boundaries
        if (password.length() < 8 || password.length() > 128) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "{\"error\":\"Password must be 8–128 characters long.\"}");
            return;
        }

        // Generate a cryptographically strong random salt
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        // Derive the password hash using PBKDF2
        byte[] hash;
        try {
            PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            hash = skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            // Do not reveal details to client
            log("Failed to hash password", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "{\"error\":\"Server error occurred.\"}");
            return;
        }

        // Encode salt and hash in Base64 for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Store into the database using a JNDI DataSource and prepared statement
        try {
            // Look up DataSource via JNDI (no hard-coded credentials)
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");

            // Use try-with-resources to ensure closure of resources
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)")) {

                // Principle of least privilege: the DB user should only have INSERT rights on users
                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.executeUpdate();
            }

            // 201 Created: registration succeeded
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("{\"message\":\"Registration successful.\"}");

        } catch (NamingException | SQLException e) {
            // Log full stacktrace to server logs; return generic message to client
            log("Database error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "{\"error\":\"Server error occurred.\"}");
        }
    }
}

