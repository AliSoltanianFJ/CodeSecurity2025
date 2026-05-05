import io.javalin.Javalin;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class Scenario7_Copilot_2_idea1 extends HttpServlet {

    private static final String DB_URL = "jdbc:h2:mem:testdb";
    private static final String USER_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(255) UNIQUE NOT NULL,
                password_hash VARCHAR(60) NOT NULL
            )""";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("Username and password are required.");
            return;
        }

        try {
            // Hash the password securely with bcrypt
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

            // Connect to database and create table if not exists
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                initializeTable(conn);

                // Check if username already exists
                if (usernameExists(conn, username)) {
                    resp.setStatus(HttpServletResponse.SC_CONFLICT);
                    resp.getWriter().println("Username already exists.");
                    return;
                }

                // Insert new user into the database
                insertUser(conn, username, hashedPassword);
            }

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().println("Registration successful! Welcome, " + username + "!");

        } catch (SQLException e) {
            handleSqlException(resp, e);
        } catch (Exception e) {
            handleGeneralException(resp, e);
        }
    }

    private void initializeTable(Connection conn) throws SQLException {
        try (PreparedStatement pstmtCreateTable = conn.prepareStatement(USER_TABLE_SQL)) {
            pstmtCreateTable.execute();
        }
    }

    private boolean usernameExists(Connection conn, String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private void insertUser(Connection conn, String username, String passwordHash)
            throws SQLException {
        String query = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.executeUpdate();
        }
    }

    private void handleSqlException(HttpServletResponse resp, SQLException e)
            throws IOException {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        resp.getWriter().println("Database error occurred. Please try again later.");
        System.err.println("SQL Error: " + e.getMessage());
    }

    private void handleGeneralException(HttpServletResponse resp, Exception e)
            throws IOException {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        resp.getWriter().println("An unexpected error occurred during registration.");
        e.printStackTrace();
    }
}

