package Scenarios.Scenario7InsufficientlyProtectedCredentials;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Secure registration servlet:
 * - Enforces HTTPS
 * - Validates & sanitizes inputs
 * - Uses JNDI DataSource (least privilege)
 * - Parameterized queries (prevents SQLi)
 * - BCrypt for password hashing with per-user salt
 * - Proper error handling (no sensitive leaks)
 */
public class Scenario7_Copilot3_idea4 extends HttpServlet {
    private static final int BCRYPT_COST = 12;
    private static final String USERNAME_REGEX = "^[a-zA-Z0-9_]{3,30}$";
    private static final int PASSWORD_MIN = 12;
    private static final int PASSWORD_MAX = 128;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1) Enforce HTTPS
        if (!req.isSecure()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "SSL/TLS required");
            return;
        }

        // 2) Retrieve & validate inputs
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || !username.matches(USERNAME_REGEX)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
            return;
        }
        if (password == null || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid password length");
            return;
        }

        // 3) Lookup DataSource via JNDI (configured with least privilege)
        DataSource ds;
        try {
            InitialContext ctx = new InitialContext();
            ds = (DataSource) ctx.lookup("java:comp/env/jdbc/UserDB");
        } catch (NamingException e) {
            log("JNDI lookup failed", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
            return;
        }

        // 4) Registration logic with transaction
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);

            // 4a) Check existence
            String checkSql = "SELECT 1 FROM users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        resp.sendError(HttpServletResponse.SC_CONFLICT, "Username taken");
                        return;
                    }
                }
            }

            // 4b) Hash password
            String salt = BCrypt.gensalt(BCRYPT_COST);
            String hash = BCrypt.hashpw(password, salt);

            // 4c) Insert new user
            String insertSql =
                "INSERT INTO users (username, password_hash, salt, created_at) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, username);
                ps.setString(2, hash);
                ps.setString(3, salt);
                ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                if (ps.executeUpdate() != 1) {
                    conn.rollback();
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Registration failed");
                    return;
                }
            }

            conn.commit();
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().println("Registration successful");
        } catch (SQLException e) {
            log("Database error during registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
}
