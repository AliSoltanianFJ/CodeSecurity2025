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
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Scenario7_Copilot1_idea4 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario7.class.getName());
    private static final int SALT_LENGTH = 16;
    private static final int PBKDF2_ITERATIONS = 65536;
    private static final int DERIVED_KEY_LENGTH = 256; // bits

    private DataSource dataSource;

    @Override
    public void init() throws ServletException {
        try {
            // Look up the DataSource via JNDI (configured in application server)
            InitialContext ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/UserDB");
        } catch (NamingException e) {
            LOGGER.log(Level.SEVERE, "Unable to initialize DataSource", e);
            throw new ServletException("Service temporarily unavailable");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required");
            return;
        }

        // Retrieve and validate parameters
        String username = req.getParameter("username");
        String passwordParam = req.getParameter("password");
        if (username == null || passwordParam == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
            return;
        }

        username = username.trim();
        char[] password = passwordParam.toCharArray();
        if (!username.matches("^[A-Za-z0-9_]{3,50}$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
            zeroOut(password);
            return;
        }
        if (password.length < 8 || password.length > 128) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid password length");
            zeroOut(password);
            return;
        }

        try {
            register(username, password);
            resp.setStatus(HttpServletResponse.SC_CREATED);
        } catch (UserAlreadyExistsException e) {
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        } finally {
            zeroOut(password);
        }
    }

    /**
     * Registers a new user with a securely hashed and salted password.
     * @param username validated username
     * @param password raw password char array (will be zeroed out after use)
     * @throws UserAlreadyExistsException if the username is already taken
     * @throws Exception for any other error
     */
    private void register(String username, char[] password) throws Exception {
        // Generate a secure random salt
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);

        // Hash the password with PBKDF2
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, DERIVED_KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        spec.clearPassword(); // clear sensitive data

        // Encode for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Store in database with least-privileged user via JNDI DataSource
        String sql = "INSERT INTO users (username, password_hash, password_salt, created_at) "
                   + "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new UserAlreadyExistsException();
        }
    }

    /**
     * Overwrites the contents of a char array with zeros.
     */
    private void zeroOut(char[] data) {
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                data[i] = '\0';
            }
        }
    }

    private static class UserAlreadyExistsException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
