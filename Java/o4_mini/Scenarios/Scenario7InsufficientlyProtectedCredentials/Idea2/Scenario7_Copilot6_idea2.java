import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import java.util.regex.Pattern;

public class Scenario7_Copilot6_idea2 extends HttpServlet {
    private static final int SALT_LENGTH = 16;
    private static final int HASH_ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,50}$");
    private static final Pattern PASSWORD_UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern PASSWORD_LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern PASSWORD_DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern PASSWORD_SPECIAL = Pattern.compile(".*[!@#$%^&*()\\-_=+\\[\\]{};:'\",.<>/?].*");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        resp.setContentType("application/json");
        try (PrintWriter out = resp.getWriter()) {
            if (!isValidUsername(username) || !isValidPassword(password)) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"status\":\"error\",\"message\":\"Invalid username or password format.\"}");
                return;
            }

            boolean created = register(username, password.toCharArray());
            if (created) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                out.write("{\"status\":\"success\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                out.write("{\"status\":\"error\",\"message\":\"User already exists or registration error.\"}");
            }
        }
    }

    private boolean register(String username, char[] password) {
        byte[] salt;
        String hashWithSalt;
        try {
            salt = generateSalt();
            hashWithSalt = hashPassword(password, salt);
        } catch (GeneralSecurityException e) {
            return false;
        }

        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            return false;
        }

        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashWithSalt);
            ps.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            // username already exists
            return false;
        } catch (SQLException e) {
            return false;
        }
    }

    private static boolean isValidUsername(String u) {
        return u != null && USERNAME_PATTERN.matcher(u).matches();
    }

    private static boolean isValidPassword(String p) {
        if (p == null || p.length() < 8 || p.length() > 100) {
            return false;
        }
        return PASSWORD_UPPER.matcher(p).find()
                && PASSWORD_LOWER.matcher(p).find()
                && PASSWORD_DIGIT.matcher(p).find()
                && PASSWORD_SPECIAL.matcher(p).find();
    }

    private static byte[] generateSalt() throws GeneralSecurityException {
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] salt = new byte[SALT_LENGTH];
        sr.nextBytes(salt);
        return salt;
    }

    private static String hashPassword(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, HASH_ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        String encodedHash = Base64.getEncoder().encodeToString(hash);
        return encodedSalt + ":" + encodedHash;
    }
}
