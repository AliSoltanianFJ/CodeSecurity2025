import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class O4_Mini_Scenario7_Copilot4_FT_MP extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(Scenario7.class);

    // Constants for PBKDF2
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH = 16;            // 128 bits
    private static final int HASH_ITERATIONS = 65536;
    private static final int HASH_KEY_LENGTH = 256;       // 256 bits

    // Username: 3–30 chars, letters, digits, underscore
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,30}$");
    // Password: enforce minimum 10 chars, at least 1 letter and 1 digit
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*()_+\\-={}\\[\\]|:;\"'<>,.?/`~]{10,}$");

    // Obtain DataSource via JNDI; configured externally in the container
    private DataSource dataSource;

    @Override
    public void init() throws ServletException {
        try {
            Context ctx = (Context) new InitialContext().lookup("java:comp/env");
            dataSource = (DataSource) ctx.lookup("jdbc/MyAppDB");
        } catch (NamingException e) {
            log.error("Unable to initialize DataSource", e);
            throw new ServletException("Internal configuration error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce UTF-8 for parameters
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");

        String username = req.getParameter("username");
        String passwordParam = req.getParameter("password");

        // 1) Validate presence
        if (username == null || passwordParam == null) {
            // Do not reveal which one is missing
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                "{\"error\":\"username and password are required\"}");
            return;
        }

        // 2) Validate format
        if (!USERNAME_PATTERN.matcher(username).matches() ||
            !PASSWORD_PATTERN.matcher(passwordParam).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "{\"error\":\"invalid username or password format\"}");
            return;
        }

        // 3) Convert password to char[] for immediate wiping
        char[] password = passwordParam.toCharArray();
        try {
            registerUser(username, password);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("{\"status\":\"user created\"}");
        } catch (UserAlreadyExistsException e) {
            resp.sendError(HttpServletResponse.SC_CONFLICT,
                "{\"error\":\"username already exists\"}");
        } catch (Exception e) {
            // Log full details, but return generic message
            log.error("Error during user registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "{\"error\":\"internal server error\"}");
        } finally {
            // Zero out password in memory
            java.util.Arrays.fill(password, '\0');
        }
    }

    /**
     * Registers a new user. Generates a secure random salt, hashes the password
     * with PBKDF2, and stores username, salt, and hash in the database.
     *
     * @param username the sanitized username
     * @param password the raw password char[] (will be wiped)
     * @throws UserAlreadyExistsException if the username is already taken
     * @throws SQLException               on database errors
     * @throws NoSuchAlgorithmException   if PBKDF2 algorithm is unavailable
     * @throws InvalidKeySpecException    on key spec issues
     */
    private void registerUser(String username, char[] password)
            throws UserAlreadyExistsException,
                   SQLException,
                   NoSuchAlgorithmException,
                   InvalidKeySpecException {
        byte[] salt = generateSalt();
        byte[] hash = pbkdf2(password, salt);

        // Base64‐encode for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        String sql = "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Principle of Least Privilege: assume the DB user has only INSERT privileges here
            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.executeUpdate();

        } catch (SQLIntegrityConstraintViolationException e) {
            // Unique constraint on username violated
            throw new UserAlreadyExistsException("Username already exists", e);
        }
    }

    /**
     * Generates a cryptographically secure random salt.
     */
    private byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom rnd = SecureRandom.getInstanceStrong(); // OS‐provided CSPRNG
        byte[] salt = new byte[SALT_LENGTH];
        rnd.nextBytes(salt);
        return salt;
    }

    /**
     * Computes a PBKDF2 hash of the password with the given salt.
     */
    private byte[] pbkdf2(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, HASH_ITERATIONS, HASH_KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        byte[] hash = skf.generateSecret(spec).getEncoded();
        spec.clearPassword(); // wipe temporary
        return hash;
    }

    /**
     * Custom exception to signal a duplicate‐username condition.
     */
    private static class UserAlreadyExistsException extends Exception {
        private static final long serialVersionUID = 1L;
        public UserAlreadyExistsException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }
}

