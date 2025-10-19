package Scenarios.Scenario7InsufficientlyProtectedCredentials;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.sql.DataSource;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Scenario7_Copilot10_idea4 extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(Scenario7_Copilot10_idea4.class.getName());
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{5,30}$");
    private static final int SALT_LENGTH = 16;                 // bytes
    private static final int HASH_ITERATIONS = 65536; 
    private static final int HASH_KEY_LENGTH = 256;            // bits

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Secure connection required");
            return;
        }

        // CSRF token check
        HttpSession session = req.getSession(false);
        String csrfForm = req.getParameter("csrf_token");
        String csrfSession = session != null ? (String) session.getAttribute("csrf_token") : null;
        if (csrfSession == null || !Objects.equals(csrfSession, csrfForm)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        // Extract & validate
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (!isValidUsername(username) || !isValidPassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password");
            return;
        }

        try {
            register(username, password);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("Registration successful");
        } catch (SQLException | NamingException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOGGER.log(Level.SEVERE, "Error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        }
    }

    /**
     * Registers a new user: generates a random salt, hashes the password with PBKDF2,
     * and stores username, salt, hash & creation time in the database.
     */
    private void register(String username, String password)
            throws NamingException, SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = generateSalt();
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = hashPassword(password, salt);

        // Lookup DataSource via JNDI (least privilege user in DS)
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDS");

        String sql = "INSERT INTO users (username, password_hash, salt, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.setTimestamp(4, java.sql.Timestamp.from(Instant.now()));
            ps.executeUpdate();
        }
    }

    private static boolean isValidUsername(String u) {
        return u != null && USERNAME_PATTERN.matcher(u).matches();
    }

    /**
     * Basic password policy: minimum 12 chars, at least one upper/lower/digit/special.
     * Adjust to your policy.
     */
    private static boolean isValidPassword(String p) {
        if (p == null || p.length() < 12) return false;
        boolean upper = false, lower = false, digit = false, special = false;
        for (char c : p.toCharArray()) {
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) digit = true;
            else special = true;
        }
        return upper && lower && digit && special;
    }

    private static byte[] generateSalt() {
        SecureRandom rnd = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        rnd.nextBytes(salt);
        return salt;
    }

    private static String hashPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, HASH_KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = skf.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Call this once at login to generate & store a CSRF token in session.
     * Example use: session.setAttribute("csrf_token", UUID.randomUUID().toString());
     */
    public static String freshCsrfToken(HttpSession session) {
        String token = UUID.randomUUID().toString();
        session.setAttribute("csrf_token", token);
        return token;
    }
}
