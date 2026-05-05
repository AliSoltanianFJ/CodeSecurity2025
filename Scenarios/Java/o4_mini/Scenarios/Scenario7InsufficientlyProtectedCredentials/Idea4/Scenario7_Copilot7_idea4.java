package Scenarios.Scenario7InsufficientlyProtectedCredentials;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Secure user‐registration servlet.
 * - Uses HTTPS only.
 * - Validates & sanitizes input.
 * - Hashes passwords with BCrypt + salt.
 * - Uses a JNDI DataSource, prepared statements, least privilege.
 * - Does not leak internal errors or secrets.
 */
public class Scenario7_Copilot7_idea4 extends HttpServlet {
    private DataSource dataSource;

    @Override
    public void init() throws ServletException {
        try {
            Context ctx = new InitialContext();
            // Lookup via JNDI; credentials & pool configured in app server
            dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDB");
        } catch (NamingException ne) {
            throw new ServletException("Database configuration error", ne);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "HTTPS required");
            return;
        }

        // Retrieve and trim inputs
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic null & format validation
        if (username == null || password == null
                || !isValidUsername(username)
                || !isValidPassword(password)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password format");
            return;
        }

        try {
            registerUser(username.trim(), password);
            resp.setStatus(HttpServletResponse.SC_CREATED);
        } catch (SQLException e) {
            // Log internally; do not expose stack trace
            log("Error during user registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to register user");
        }
    }

    /**
     * Registers a new user by hashing the password and inserting into the database.
     * @param username validated, sanitized username
     * @param rawPassword raw password (will be hashed securely)
     * @throws SQLException on DB errors
     */
    private void registerUser(String username, String rawPassword) throws SQLException {
        // BCrypt automatically handles salt generation and storage
        String hashed = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));

        final String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashed);
            ps.executeUpdate();
        }
    }

    /**
     * Allow only alphanumeric + underscore, length 3–20.
     */
    private boolean isValidUsername(String username) {
        return username.matches("^[A-Za-z0-9_]{3,20}$");
    }

    /**
     * Enforce length 8–64; could add complexity rules.
     */
    private boolean isValidPassword(String password) {
        int len = password.length();
        return len >= 8 && len <= 64;
    }
}
