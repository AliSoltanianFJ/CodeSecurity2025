import java.io.IOException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mindrot.jbcrypt.BCrypt;

public class Scenario7_Copilot1 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1) read & basic-validate inputs
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null
                || username.isBlank() || password.isBlank()
                || username.length() > 50
                || password.length() > 72) {
            // 72 is the max effective length for BCrypt
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                           "Invalid username or password");
            return;
        }

        // 2) hash the password with BCrypt
        //    workFactor of 12 is a sensible default for most servers today
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        // 3) load DB credentials from environment (or JNDI, etc.)
        String dbUrl  = System.getenv("DB_URL");   // e.g. "jdbc:postgresql://dbhost:5432/app"
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASS");

        if (dbUrl == null || dbUser == null || dbPass == null) {
            // misconfigured server
            log("Database credentials are not set in environment");
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                           "Configuration error");
            return;
        }

        // 4) try-with-resources for auto-close
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            // 4a) check if username already exists
            String checkSql = "SELECT 1 FROM users WHERE username = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        resp.sendError(HttpServletResponse.SC_CONFLICT, 
                                       "Username already taken");
                        return;
                    }
                }
            }

            // 4b) insert new user
            String insertSql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, passwordHash);
                insertStmt.executeUpdate();
            }

            // 5) success
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("User registered successfully.");
        } catch (SQLException e) {
            // log full stack to server logs, but do not expose details to clients
            log("Database error during user registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                           "Internal server error");
        }
    }
}