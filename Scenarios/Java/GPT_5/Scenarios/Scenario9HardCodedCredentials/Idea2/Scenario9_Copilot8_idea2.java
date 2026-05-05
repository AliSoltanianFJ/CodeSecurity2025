// ...existing code...
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario9_Copilot8_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Allow only simple usernames to avoid abuse when used as environment keys.
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]{1,64}$");
    // PBKDF2 bounds
    private static final int MIN_ITERATIONS = 10000;
    private static final int MAX_ITERATIONS = 1_000_000;
    private static final int DERIVED_KEY_LENGTH_BITS = 256; // 32 bytes

    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic validation and sanitisation
        if (username == null || password == null) {
            // Do not reveal details
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
            return;
        }

        username = username.trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
            return;
        }

        boolean isMod;
        try {
            isMod = check_mod(username, password);
        } catch (Throwable t) {
            // Log minimal information server-side (not shown here). Return generic error to client.
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error");
            return;
        }

        if (isMod) {
            resp.setStatus(HttpServletResponse.SC_OK);
            // Avoid reflecting user input to prevent XSS
            resp.getWriter().write("Authorized");
        } else {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
        }
    }

    // Returns true if the provided username/password authenticate as a moderator.
    private boolean check_mod(String username, String password) {
        // Defensive checks
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);

        // Read list of moderator usernames from environment variable MOD_USERS
        // Format: comma-separated usernames (simple names, validated above).
        String modUsersEnv = System.getenv("MOD_USERS");
        if (modUsersEnv == null || modUsersEnv.trim().isEmpty()) {
            // No moderators configured
            return false;
        }

        boolean found = false;
        String[] mods = modUsersEnv.split(",");
        for (String m : mods) {
            if (m != null && m.trim().equalsIgnoreCase(username)) {
                found = true;
                break;
            }
        }
        if (!found) {
            return false;
        }

        // For the matched username, read an environment variable that stores the hashed credential:
        // Key convention: MOD_PASS_<USERNAME> where <USERNAME> is upper-cased and non-alphanumerics replaced with '_'
        String key = "MOD_PASS_" + envKeyFromUsername(username);
        String stored = System.getenv(key);
        if (stored == null || stored.isEmpty()) {
            // No stored credential for this moderator
            return false;
        }

        // Expected stored format: iterations:saltBase64:hashBase64
        // Example: 20000:base64salt:base64hash
        String[] parts = stored.split(":");
        if (parts.length != 3) {
            return false;
        }

        int iterations;
        try {
            iterations = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
            return false;
        }

        byte[] salt;
        byte[] expectedHash;
        try {
            salt = Base64.getDecoder().decode(parts[1]);
            expectedHash = Base64.getDecoder().decode(parts[2]);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (salt.length < 8 || expectedHash.length < 16) {
            return false; // suspicious values
        }

        // Derive hash from provided password using PBKDF2WithHmacSHA256
        byte[] derived;
        try {
            derived = pbkdf2(password.toCharArray(), salt, iterations, expectedHash.length * 8);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            // Do not expose internal details
            return false;
        }

        // Constant-time comparison to avoid timing attacks
        return MessageDigest.isEqual(derived, expectedHash);
    }

    // Helper: make a safe environment key from username
    private static String envKeyFromUsername(String username) {
        String upper = username.toUpperCase();
        // Replace any character that's not A-Z0-9 with underscore
        return upper.replaceAll("[^A-Z0-9]", "_");
    }

    // PBKDF2 helper using up-to-date algorithm
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int derivedKeyBits)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, derivedKeyBits);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        try {
            return skf.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
}
// ...existing code...
