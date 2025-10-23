import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot_7_idea3 extends HttpServlet {
private static final String DB_URL = System.getenv("DATABASE_URL");
private static final String DB_USER = System.getenv("DATABASE_USER");
private static final String DB_PASS = System.getenv("DATABASE_PASSWORD");

@Override
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // Get email from request parameter, validate and sanitize
    String email = req.getParameter("email");
    if (email == null || email.trim().isEmpty() || !isValidEmail(email)) {
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        resp.getWriter().write("Invalid or missing email parameter.");
        return;
    }
    email = email.trim().toLowerCase();

    // Initialize response writer
    resp.setContentType("text/html; charset=UTF-8");
    PrintWriter out = resp.getWriter();

    try (Connection conn = getConnection()) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.write("Subscription not found for this email address.");
                    return;
                }
            }
        }

        sql = "UPDATE users SET is_subscribed = FALSE WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                out.write("Successfully unsubscribed.");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("Failed to unsubscribe.");
            }
        }
    } catch (SQLException e) {
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        out.write("Database error occurred.");
        e.printStackTrace();
    } finally {
        out.close();
    }
}

/**
 * Validate email format using a simple regex.
 * For production, consider using a more robust library.
 */
private boolean isValidEmail(String email) {
    if (email == null || email.length() > 254) return false;
    String regex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    return email.matches(regex);
}

/**
 * Retrieves a database connection.
 * In production, use a connection pool (e.g., HikariCP) to avoid overhead.
 */
private Connection getConnection() throws SQLException {
    try {
        Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException e) {
        throw new SQLException("MySQL JDBC Driver not found.");
    }
    return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
}

}