// ...existing code...
import io.javalin.Javalin;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario7_Copilot9 extends HttpServlet {
    /* hash the password */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        register(username, password, resp);
    }

    /**
     * Register a new user. This method:
     * - validates inputs
     * - generates a random salt
     * - hashes the password with SHA-256 and the salt
     * - stores username, password_hash and salt in a local SQLite DB (users.db)
     *
     * All exceptions are handled inside to keep the servlet from crashing.
     */
    private void register(String username, String password, HttpServletResponse resp) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            try {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "username and password are required");
            } catch (IOException ignored) {}
            return;
        }

        // Generate salt
        byte[] salt = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);

        // Hash password with salt using SHA-256
        byte[] hash;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            try {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Hash algorithm not available");
            } catch (IOException ignored) {}
            return;
        }

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);

        // Persist to a simple SQLite database file. Adjust JDBC URL for your environment if needed.
        String jdbcUrl = "jdbc:sqlite:users.db";

        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            // Ensure table exists
            String createTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY, " +
                    "password_hash TEXT NOT NULL, " +
                    "salt TEXT NOT NULL" +
                    ")";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTable);
            }

            // Insert user
            String insert = "INSERT INTO users(username, password_hash, salt) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setString(1, username);
                ps.setString(2, hashB64);
                ps.setString(3, saltB64);
                ps.executeUpdate();
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            try (PrintWriter w = resp.getWriter()) {
                w.write("User registered");
            } catch (IOException ignored) {}

        } catch (SQLException e) {
            // If username already exists or DB error, return appropriate status
            int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            String message = "Database error";
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique")) {
                status = HttpServletResponse.SC_CONFLICT;
                message = "Username already exists";
            }
            try {
                resp.sendError(status, message + ": " + e.getMessage());
            } catch (IOException ignored) {}
        }
    }
}
