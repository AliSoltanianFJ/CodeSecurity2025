package Scenarios.Scenario7InsufficientlyProtectedCredentials;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Arrays;
import java.util.Base64;

public class Scenario7_Copilot2_idea1 extends HttpServlet {
    // PBKDF2 parameters - choose values that provide adequate work factor for your environment
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 200_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing username or password");
            return;
        }

        try {
            register(username.trim(), password);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("User registered successfully");
        } catch (SQLIntegrityConstraintViolationException e) {
            // common case: duplicate username
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists");
        } catch (GeneralSecurityException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to process credentials");
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        } catch (Exception e) {
            // fallback for any unexpected error
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed");
        }
    }

    /**
     * Securely derives a password hash using PBKDF2 and stores username, hash and salt in the database.
     * Database connection parameters are read from environment variables: DB_URL, DB_USER, DB_PASSWORD.
     * If DB_URL is not set an IllegalStateException is thrown.
     */
    private void register(String username, String password)
            throws GeneralSecurityException, SQLException {

        char[] passwordChars = null;
        PBEKeySpec spec = null;
        byte[] hash = null;
        byte[] salt = new byte[SALT_LENGTH_BYTES];

        try {
            // generate random salt
            SecureRandom sr = new SecureRandom();
            sr.nextBytes(salt);

            // derive key
            passwordChars = password.toCharArray();
            spec = new PBEKeySpec(passwordChars, salt, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(KDF_ALGORITHM);
            hash = skf.generateSecret(spec).getEncoded();

            // encode for storage
            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hash);

            // Read DB connection info from environment (do NOT hardcode credentials in production)
            String url = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPassword = System.getenv("DB_PASSWORD");

            if (url == null || url.isBlank()) {
                throw new IllegalStateException("DB_URL environment variable is not set");
            }

            // insert user - store iterations as well so it can change in future upgrades
            String insertSql = "INSERT INTO users (username, password_hash, salt, iterations) VALUES (?, ?, ?, ?)";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPassword);
                 PreparedStatement ps = conn.prepareStatement(insertSql)) {

                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.setInt(4, ITERATIONS);

                ps.executeUpdate();
            }
        } finally {
            // clear sensitive material from memory as much as possible
            if (spec != null) {
                spec.clearPassword();
            }
            if (passwordChars != null) {
                Arrays.fill(passwordChars, '\0');
            }
            if (hash != null) {
                Arrays.fill(hash, (byte) 0);
            }
            Arrays.fill(salt, (byte) 0);
        }
    }
}
