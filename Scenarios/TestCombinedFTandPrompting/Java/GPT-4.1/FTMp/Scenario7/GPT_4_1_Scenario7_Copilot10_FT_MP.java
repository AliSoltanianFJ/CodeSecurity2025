import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/register")
public class GPT_4_1_Scenario7_Copilot10_FT_MP extends HttpServlet {
    // Security decisions:
    // - All user inputs are validated and sanitized.
    // - Passwords are hashed with PBKDF2 using a unique salt per user (OWASP recommendation).
    // - No secrets or credentials are hardcoded.
    // - Error messages are generic, no sensitive info leaked.
    // - Principle of least privilege assumed for DB user.
    // - Secure logging: no sensitive data in logs.
    // - All configs should enforce HTTPS (not shown here).
    // - Prepared statements prevent SQL injection (CWE-89).
    // - No insecure cryptography or deprecated APIs.
    // - XSS/CSRF protections assumed at higher layers (CSP, SameSite cookies, etc).

    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario7_Copilot10_FT_MP.class.getName());

    // Username: 3-32 chars, alphanum + _-
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,32}$");
    // Password: min 12 chars, at least 1 upper, 1 lower, 1 digit, 1 special
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-])[A-Za-z\\d@$!%*?&#^()_+=-]{12,64}$"
    );

    // PBKDF2 config (OWASP recommended values)
    private static final int SALT_LENGTH = 32; // bytes
    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256; // bits

    private DataSource dataSource;

    @Override
    public void init() throws ServletException {
        // Principle of least privilege: DataSource should be configured with minimal DB permissions
        ServletContext context = getServletContext();
        Object ds = context.getAttribute("SECURE_DB");
        if (ds instanceof DataSource) {
            dataSource = (DataSource) ds;
        } else {
            throw new ServletException("Secure DataSource not configured");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce HTTPS (should be done at server config, but double-check)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required");
            return;
        }

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Validate inputs
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
            return;
        }
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Password does not meet complexity requirements");
            return;
        }

        // Sanitize username (defense in depth, though prepared statements are used)
        String sanitizedUsername = username.trim();

        // Hash password securely
        byte[] salt = new byte[SALT_LENGTH];
        try {
            SecureRandom sr = SecureRandom.getInstanceStrong();
            sr.nextBytes(salt);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "SecureRandom not available", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
            return;
        }

        byte[] hash;
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            hash = skf.generateSecret(spec).getEncoded();
            spec.clearPassword();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOGGER.log(Level.SEVERE, "Password hashing failed", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
            return;
        }

        // Encode salt and hash for storage (Base64, not hex, to avoid charset issues)
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Store in DB securely
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO users (username, password_hash, password_salt) VALUES (?, ?, ?)"
             )) {
            ps.setString(1, sanitizedUsername);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Handle duplicate username (do not leak details)
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) { // Integrity constraint violation
                resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists");
            } else {
                LOGGER.log(Level.SEVERE, "DB error", e);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error");
            }
            return;
        }

        // Success - do not reveal any sensitive info
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write("Registration successful. Please log in.");
    }
}
