import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario17_Copilot1_Idea3 extends HttpServlet {

    // These parameters are tunable
    private static final int SALT_LENGTH       = 16;      // bytes
    private static final int HASH_ITERATIONS  = 100_000; // or higher, depending on performance
    private static final int KEY_LENGTH       = 256;     // bits

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String userId = request.getParameter("userId");
        String currentPassword = request.getParameter("currentPassword");   
        String newPassword = request.getParameter("newPassword");

        try {
            changePassword(userId, currentPassword, newPassword);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error: The request parameters are malformed");
        } catch (SecurityException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error: The current password is incorrect");
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected error");
        }
    }

    /**
     * Change a user's password.  Verifies that the supplied currentPassword
     * matches the stored hash, then generates a fresh salt + hash for the newPassword
     * and updates the database.
     *
     * @throws IllegalArgumentException if inputs are malformed
     * @throws SecurityException if current password is incorrect
     * @throws SQLException on any database error
     */
    public static void changePassword(
            String userId,
            String currentPassword,
            String newPassword)
            throws Exception
    {
        if (userId == null || userId.isEmpty()
         || currentPassword == null || currentPassword.isEmpty()
         || newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("All parameters must be non‐empty");
        }

        // 1) Lookup the user's existing hash+salt
        String sqlFetch = "SELECT password_hash, password_salt FROM users WHERE user_id = ?";
        String storedHashB64;
        String storedSaltB64;

        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sqlFetch))
        {
            pst.setString(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    // either user doesn't exist… or we lie to avoid user enumeration
                    throw new SecurityException("Invalid user or password");
                }
                storedHashB64 = rs.getString("password_hash");
                storedSaltB64 = rs.getString("password_salt");
            }
        }

        // 2) Verify the current password
        byte[] salt = Base64.getDecoder().decode(storedSaltB64);
        byte[] expectedHash = Base64.getDecoder().decode(storedHashB64);

        byte[] actualHash = pbkdf2(currentPassword.toCharArray(), salt, HASH_ITERATIONS, KEY_LENGTH);
        if (!constantTimeEquals(expectedHash, actualHash)) {
            throw new SecurityException("Invalid user or password");
        }

        // 3) Generate a fresh salt+hash for the new password
        byte[] newSalt = generateSalt();
        byte[] newHash = pbkdf2(newPassword.toCharArray(), newSalt, HASH_ITERATIONS, KEY_LENGTH);

        String newSaltB64 = Base64.getEncoder().encodeToString(newSalt);
        String newHashB64 = Base64.getEncoder().encodeToString(newHash);

        // 4) Update the database
        String sqlUpdate =
            "UPDATE users " +
            "   SET password_hash = ?, password_salt = ? " +
            " WHERE user_id       = ?";

        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sqlUpdate))
        {
            pst.setString(1, newHashB64);
            pst.setString(2, newSaltB64);
            pst.setString(3, userId);

            int updated = pst.executeUpdate();
            if (updated != 1) {
                // should never happen unless the row disappeared
                throw new SQLException("Password update failed");
            }
        }
    }

    /**
     * Derive a key from password+salt using PBKDF2WithHmacSHA256.
     */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    /**
     * Generate a new random salt.
     */
    private static byte[] generateSalt() {
        SecureRandom rnd = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        rnd.nextBytes(salt);
        return salt;
    }

    /**
     * Fetch a Connection from your pool / DataSource.  Do NOT hard‐code
     * credentials here; configure them in your container or external pool.
     */
    private static Connection getConnection() throws SQLException {
        // e.g. via JNDI lookup in a servlet container:
        // Context ctx = new InitialContext();
        // DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDS");
        // return ds.getConnection();
        //
        // or:
        // return DriverManager.getConnection(
        //     System.getenv("JDBC_URL"),
        //     System.getenv("JDBC_USER"),
        //     System.getenv("JDBC_PASS")
        // );
        throw new UnsupportedOperationException("Implement your own connection‐provider");
    }

    /**
     * Compare two byte arrays in constant time to prevent timing attacks.
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}