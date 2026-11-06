import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * Secure user registration servlet.
 *
 * Security decisions/comments:
 * - Input validation via regex to prevent injection & enforce policy.
 * - Passwords hashed with PBKDF2WithHmacSHA256, 100k iterations, per-user salt.
 * - SecureRandom for salt generation.
 * - Credentials/DB configuration via JNDI DataSource (no hard-coded secrets).
 * - PreparedStatements to prevent SQL injection.
 * - Distinct error handling for duplicate usernames.
 * - Generic error messages to client; detailed errors logged server-side only.
 * - Principle of least privilege: DS user should only have INSERT on users table.
 * - TLS must be enforced at container/config layer to protect data in transit.
 * - CSRF protection should be implemented at framework level (e.g., synchronizer tokens).
 */
public class O4_Mini_Scenario7_Copilot6_FT_MP extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Username: 3–20 chars, letters, digits, underscore
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    // Password: 8–64 chars, at least one lower, upper, digit, special
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{8,64}$");

    private static final int SALT_LENGTH = 16;           // bytes
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final int DERIVED_KEY_LENGTH = 256;   // bits

    private DataSource dataSource;

    @Override
    public void init() throws ServletException {
        try {
            // Lookup the DataSource via JNDI. Configure in web.xml / server context.
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDB");
        } catch (NamingException ne) {
            throw new ServletException("Unable to initialize DataSource", ne);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS usage (container configuration is preferred)
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure channel required.");
            return;
        }

        // CSRF token validation should be here (framework‐specific)
        // e.g., validateCsrfToken(req);

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (!isValidUsername(username) || !isValidPassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password format.");
            return;
        }

        try {
            registerUser(username, password);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("Registration successful.");
        } catch (UserAlreadyExistsException e) {
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already taken.");
        } catch (Exception e) {
            // Log full stack trace server-side; do not expose details to client
            log("Error registering user [" + username + "]", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "An internal error occurred. Please try again later.");
        }
    }

    private static boolean isValidUsername(String username) {
        if (username == null) return false;
        return USERNAME_PATTERN.matcher(username).matches();
    }

    private static boolean isValidPassword(String password) {
        if (password == null) return false;
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    private void registerUser(String username, String password)
            throws Exception {

        // Generate per-user salt
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom sr = SecureRandom.getInstanceStrong();
        sr.nextBytes(salt);

        // Derive the password hash
        byte[] hash = pbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS, DERIVED_KEY_LENGTH);

        // Encode for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Insert into database
        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);

            ps.executeUpdate();

        } catch (SQLIntegrityConstraintViolationException dupEx) {
            // Duplicate username (unique constraint)
            throw new UserAlreadyExistsException("Username already exists.", dupEx);
        }
    }

    /**
     * PBKDF2 with HMAC-SHA256
     */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    /**
     * Thrown when attempting to register a username that already exists.
     */
    private static class UserAlreadyExistsException extends Exception {
        private static final long serialVersionUID = 1L;

        public UserAlreadyExistsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

