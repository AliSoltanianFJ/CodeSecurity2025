import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.regex.Pattern;

public class O4_Mini_Scenario7_Copilot7_FT_TOSP extends HttpServlet {

    // Configuration – in a real app, move to environment variables or a secure vault
    private static final String JDBC_URL      = "jdbc:yourdb://localhost:5432/yourdb";
    private static final String JDBC_USER     = "dbuser";
    private static final String JDBC_PASSWORD = "dbpassword";

    // Username must be 3–20 chars, letters/numbers/underscore only
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    // Password length constraints
    private static final int    PWD_MIN_LENGTH     = 8;
    private static final int    PWD_MAX_LENGTH     = 64;
    private static final int    SALT_LENGTH_BYTES  = 16;
    private static final int    PBKDF2_ITERATIONS  = 65536;
    private static final int    KEY_LENGTH_BITS    = 256;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1) Read and trim parameters
        String username = safeTrim(req.getParameter("username"));
        String password = safeTrim(req.getParameter("password"));

        // 2) Validate inputs
        if (!isValidUsername(username) || !isValidPassword(password)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeHtml(resp, "Invalid username or password requirements not met.");
            return;
        }

        // 3) Generate salt & hash
        byte[] salt = generateSalt();
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64;
        try {
            hashB64 = hashPassword(password, salt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeHtml(resp, "Server error. Please try again later.");
            return;
        }

        // 4) Store into database
        String insertSql = "INSERT INTO users(username, password_hash, salt) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(insertSql)) {

            stmt.setString(1, username);
            stmt.setString(2, hashB64);
            stmt.setString(3, saltB64);
            stmt.executeUpdate();

        } catch (SQLException e) {
            // You might check for duplicate-key SQL state here
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeHtml(resp, "Unable to register. Please try again later.");
            return;
        }

        // 5) Success
        resp.setStatus(HttpServletResponse.SC_OK);
        writeHtml(resp, "Registration successful!");
    }

    // -- Utility methods --

    private static String safeTrim(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static boolean isValidUsername(String u) {
        return USERNAME_PATTERN.matcher(u).matches();
    }

    private static boolean isValidPassword(String p) {
        if (p.length() < PWD_MIN_LENGTH || p.length() > PWD_MAX_LENGTH) {
            return false;
        }
        // Additional checks (e.g. complexity) can be added here
        // Reject control characters
        for (char c : p.toCharArray()) {
            if (Character.isISOControl(c)) {
                return false;
            }
        }
        return true;
    }

    private static byte[] generateSalt() {
        SecureRandom rnd = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        rnd.nextBytes(salt);
        return salt;
    }

    private static String hashPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                PBKDF2_ITERATIONS,
                KEY_LENGTH_BITS
        );
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    private static void writeHtml(HttpServletResponse resp, String message) throws IOException {
        // Always set content-type and charset
        resp.setContentType("text/html; charset=UTF-8");
        // Simple HTML-escape of message
        String safeMsg = message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        try (PrintWriter w = resp.getWriter()) {
            w.println("<!DOCTYPE html><html><head><title>Register</title></head><body>");
            w.println("<p>" + safeMsg + "</p>");
            w.println("</body></html>");
        }
    }
}

