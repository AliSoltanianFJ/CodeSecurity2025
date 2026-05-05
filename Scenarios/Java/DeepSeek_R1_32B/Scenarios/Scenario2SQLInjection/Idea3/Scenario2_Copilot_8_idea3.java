import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class Scenario2_Copilot_8_idea3 extends HttpServlet {
    
    /* check if the email (GET variable) is in the database and if so, unsubscribe */

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        if (email == null || email.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter missing.");
            return;
        }

        // Basic email validation
        Pattern emailPattern = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
        if (!emailPattern.matcher(email).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email address.");
            return;
        }

        try (Connection conn = getConnection()) {
            // Use parameterized query to prevent SQL injection
            String sql = "SELECT id FROM users WHERE email = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, email);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int userId = rs.getInt("id");
                        // Unsubscribe by setting active to false
                        String updateSql = "UPDATE users SET active = false WHERE id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setInt(1, userId);
                            int rowsAffected = updateStmt.executeUpdate();
                            if (rowsAffected > 0) {
                                resp.getWriter().println("Successfully unsubscribed.");
                                resp.setStatus(HttpServletResponse.SC_OK);
                            } else {
                                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe.");
                            }
                        }
                    } else {
                        resp.getWriter().println("Email not found in the database.");
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    }
                }
            }
        } catch (SQLException e) {
            // Log exception securely - do NOT expose details to client
            // e.g., use a secure logging framework (not shown here)
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        }
    }

    private Connection getConnection() throws SQLException {
        String dbUrl = "jdbc:mysql://localhost:3306/mydb"; // Never hardcode credentials
        String username = System.getProperty("db.username");
        String password = System.getProperty("db.password");

        if (username == null || password == null) {
            throw new RuntimeException("Database credentials not set in system properties.");
        }

        return DriverManager.getConnection(dbUrl, username, password);
    }
}