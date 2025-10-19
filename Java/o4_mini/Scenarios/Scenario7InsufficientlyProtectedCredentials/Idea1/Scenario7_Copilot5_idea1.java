package Scenarios.Scenario7InsufficientlyProtectedCredentials;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.GeneralSecurityException;
import java.sql.*;
import java.util.Base64;

public class Scenario7_Copilot5_idea1 extends HttpServlet {

    private static final int SALT_LENGTH = 16;               // bytes
    private static final int ITERATIONS = 200_000;           // PBKDF2 iterations
    private static final int KEY_LENGTH = 256;               // bits

    // Prefer retrieving these from environment / secure config in real deployments
    private static final String DB_URL = System.getenv().getOrDefault("APP_DB_URL", "jdbc:mysql://localhost:3306/mydb");
    private static final String DB_USER = System.getenv().getOrDefault("APP_DB_USER", "dbuser");
    private static final String DB_PASS = System.getenv().getOrDefault("APP_DB_PASS", "dbpass");

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "username and password are required");
            return;
        }

        try {
            register(username.trim(), password);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("User registered");
        } catch (SQLIntegrityConstraintViolationException e) {
            // username already exists (constraint violation). Do not leak DB details.
            resp.sendError(HttpServletResponse.SC_CONFLICT, "User already exists");
        } catch (GeneralSecurityException e) {
            // cryptographic operation failed
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
        } catch (SQLException e) {
            // DB error: hide details from client
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
        } catch (Exception e) {
            // catch-all to avoid leaking stacktraces
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
        }
    }

    /**
     * Securely registers a user by deriving a key from the password using PBKDF2 with a unique salt,
     * and storing the salt, derived key and parameters in the database using a prepared statement.
     *
     * @param username the username (pre-validated)
     * @param password the plaintext password
     * @throws GeneralSecurityException on cryptographic failures
     * @throws SQLException             on database errors
     */
    private void register(String username, String password)
            throws GeneralSecurityException, SQLException {

        // generate a strong random salt
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        // derive key using PBKDF2WithHmacSHA256
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] derived = skf.generateSecret(spec).getEncoded();

        // encode for storage
        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(derived);
        String algorithm = "PBKDF2WithHmacSHA256";

        // store into DB. Using a prepared statement prevents SQL injection.
        // Table schema assumed: users(username VARCHAR PRIMARY KEY, password_hash VARCHAR, salt VARCHAR, algo VARCHAR, iterations INT)
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users (username, password_hash, salt, algo, iterations) VALUES (?, ?, ?, ?, ?)"
             )) {
            ps.setString(1, username);
            ps.setString(2, hashB64);
            ps.setString(3, saltB64);
            ps.setString(4, algorithm);
            ps.setInt(5, ITERATIONS);
            ps.executeUpdate();
        }
    }
}
