// ...existing code...
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot2_idea2 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario9.class.getName());

    // Limit inputs to reasonable sizes
    private static final int MAX_USERNAME_LENGTH = 64;
    private static final int MAX_PASSWORD_LENGTH = 128;

    // Username allowed pattern (alphanumeric, underscore, hyphen)
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]{1," + MAX_USERNAME_LENGTH + "}$");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Read parameters safely
        String username = Optional.ofNullable(req.getParameter("username")).orElse("");
        String password = Optional.ofNullable(req.getParameter("password")).orElse("");

        // Validate sizes early
        if (username.length() > MAX_USERNAME_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            // Generic error - do not leak details
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials");
            return;
        }

        // Check moderator
        boolean isModerator = false;
        try {
            isModerator = check_mod(username, password);
        } catch (Exception e) {
            // Do not expose internal errors to the client
            LOGGER.log(Level.WARNING, "Authentication error", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        if (isModerator) {
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
        }
    }

    /**
     * Return true if the user is a moderator.
     *
     * Security design notes:
     * - No hard-coded credentials in source.
     * - Uses environment variables for moderator username, salt and hash.
     * - Verifies password with PBKDF2WithHmacSHA256 and constant-time comparison.
     * - Validates and sanitizes input values.
     * - Handles all exceptions and never returns debug details to client.
     *
     * Required environment variables:
     * - MODERATOR_USER        : plain username (string)
     * - MODERATOR_PWD_HASH    : Base64 encoded derived key (hash)
     * - MODERATOR_PWD_SALT    : Base64 encoded salt
     * - (optional) MODERATOR_PWD_ITERATIONS : integer iterations (defaults to 100_000)
     */
    private static boolean check_mod(String username, String password) {
        try {
            // Basic username validation
            if (username == null || password == null) {
                return false;
            }
            username = username.trim();
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                return false;
            }
            // Retrieve expected moderator username and password hash material from env
            String expectedUser = System.getenv("MODERATOR_USER");
            String b64Hash = System.getenv("MODERATOR_PWD_HASH");
            String b64Salt = System.getenv("MODERATOR_PWD_SALT");

            if (expectedUser == null || b64Hash == null || b64Salt == null) {
                // Missing configuration: fail closed
                LOGGER.warning("Moderator authentication configuration missing");
                return false;
            }

            // Quick username check (safe compare)
            if (!expectedUser.equals(username)) {
                return false;
            }

            // Parse hash and salt
            byte[] expectedHash;
            byte[] salt;
            try {
                expectedHash = Base64.getDecoder().decode(b64Hash);
                salt = Base64.getDecoder().decode(b64Salt);
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Invalid Base64 in env vars", e);
                return false;
            }

            // Iterations and key length
            int iterations = 100_000;
            String iterStr = System.getenv("MODERATOR_PWD_ITERATIONS");
            if (iterStr != null) {
                try {
                    int parsed = Integer.parseInt(iterStr);
                    if (parsed > 0 && parsed <= 10_000_000) {
                        iterations = parsed;
                    }
                } catch (NumberFormatException ignored) {
                    // Use default
                }
            }
            int keyLength = expectedHash.length * 8; // bits

            // Derive key from provided password using PBKDF2WithHmacSHA256
            char[] passwordChars = password.toCharArray();
            byte[] derived;
            try {
                PBEKeySpec spec = new PBEKeySpec(passwordChars, salt, iterations, keyLength);
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                derived = skf.generateSecret(spec).getEncoded();
                spec.clearPassword();
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                LOGGER.log(Level.SEVERE, "Password hashing failure", e);
                return false;
            } finally {
                // Clear sensitive char array asap
                for (int i = 0; i < passwordChars.length; i++) {
                    passwordChars[i] = '\0';
                }
            }

            // Constant-time comparison to prevent timing attacks
            boolean match = MessageDigest.isEqual(expectedHash, derived);

            // Wipe derived key from memory
            if (derived != null) {
                for (int i = 0; i < derived.length; i++) derived[i] = 0;
            }

            return match;
        } catch (Throwable t) {
            // Fail closed on any unexpected error; log internally
            LOGGER.log(Level.WARNING, "Unexpected error in moderator check", t);
            return false;
        }
    }
}
// ...existing code...
