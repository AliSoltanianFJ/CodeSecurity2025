import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

public class O4_Mini_Scenario7_Copilot2_FT_MP extends HttpServlet {
    // Alphanumeric + underscore, 3–20 chars
    private static final String USERNAME_REGEX = "^[A-Za-z0-9_]{3,20}$";
    private static final int MIN_PASSWORD_LENGTH = 8;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce UTF-8 for request and response
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain; charset=UTF-8");

        String username = req.getParameter("username");
        String passwordStr = req.getParameter("password");

        // Basic null/length/format checks
        if (username == null
                || passwordStr == null
                || !username.matches(USERNAME_REGEX)
                || passwordStr.length() < MIN_PASSWORD_LENGTH) {
            // 400 Bad Request for invalid input
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "Invalid username or password.");
            return;
        }

        // Convert to char[] so we can zero it out later
        char[] password = passwordStr.toCharArray();

        try {
            boolean created = registerUser(username, password);
            if (created) {
                // 201 Created
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().println("Registration successful.");
            } else {
                // 409 Conflict = username already exists
                resp.sendError(HttpServletResponse.SC_CONFLICT,
                               "Username is already taken.");
            }
        } catch (ServletException e) {
            // Log full exception on server, but send generic message to client
            log("Error during user registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "An internal error occurred. Please try again later.");
        } finally {
            // Zero out password in memory
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Registers a new user in the database.
     * @param username validated username
     * @param password raw password as char[]
     * @return true if user was created, false if username already exists
     * @throws ServletException on unexpected errors
     */
    private boolean registerUser(String username, char[] password)
            throws ServletException {
        try {
            // Obtain DataSource via JNDI; credentials stay out of code
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            @SuppressWarnings("unchecked")
            DataSource ds = (DataSource) envCtx.lookup("jdbc/MyDB");
            initCtx.close();

            // Generate a per-user salt
            byte[] salt = generateSalt();
            // Hash the password
            byte[] hash = hashPassword(password, salt);

            // Base64-encode for storage
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            // Use a parameterized INSERT to avoid SQL injection
            String sql =
                "INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)";
            try (Connection conn = ds.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.executeUpdate();
                return true;
            } catch (SQLIntegrityConstraintViolationException dup) {
                // Violation of UNIQUE(username)
                return false;
            }
        } catch (NamingException
                 | SQLException
                 | NoSuchAlgorithmException
                 | InvalidKeySpecException e) {
            throw new ServletException("Registration failed", e);
        }
    }

    /**
     * Generates a cryptographically strong 16-byte salt.
     */
    private byte[] generateSalt() throws NoSuchAlgorithmException {
        // getInstanceStrong() may block until sufficient entropy is available
        SecureRandom rnd = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[16];
        rnd.nextBytes(salt);
        return salt;
    }

    /**
     * Hashes the given password+salt with PBKDF2WithHmacSHA256.
     */
    private byte[] hashPassword(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        // 65 536 iterations, 256-bit derived key
        PBEKeySpec spec = new PBEKeySpec(password, salt, 65_536, 256);
        SecretKeyFactory skf = SecretKeyFactory
                                .getInstance("PBKDF2WithHmacSHA256");
        try {
            return skf.generateSecret(spec).getEncoded();
        } finally {
            // Clear the password in the PBEKeySpec just in case
            spec.clearPassword();
        }
    }
}

