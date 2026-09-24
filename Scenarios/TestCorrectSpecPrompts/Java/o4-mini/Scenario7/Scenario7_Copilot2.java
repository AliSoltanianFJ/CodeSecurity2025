import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

public class Scenario7_Copilot2 extends HttpServlet {
    private static final int BCRYPT_COST = 12;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // 1) Basic input validation
        if (username == null || username.trim().isEmpty() ||
            password == null || password.length() < 8) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "Username and password (min length 8) are required");
            return;
        }

        // 2) Hash the password with BCrypt
        String salt = BCrypt.gensalt(BCRYPT_COST);
        String hashedPassword = BCrypt.hashpw(password, salt);

        // 3) Read DB credentials from environment (or a secure vault/config)
        String dbUrl  = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD");
        if (dbUrl == null || dbUser == null || dbPass == null) {
            log("Database configuration is missing");
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Internal server error");
            return;
        }

        // 4) Register the user
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            // 4a) Optional: check for existing username to avoid duplicates
            String checkSql = "SELECT 1 FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        resp.sendError(HttpServletResponse.SC_CONFLICT,
                                       "Username already exists");
                        return;
                    }
                }
            }

            // 4b) Insert new user
            String insertSql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, hashedPassword);
                insertStmt.executeUpdate();
            }

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("User registered successfully");

        } catch (SQLException e) {
            // Log full stack trace on the server, but send only a generic message
            log("Error registering user", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Internal server error");
        }
    }
}