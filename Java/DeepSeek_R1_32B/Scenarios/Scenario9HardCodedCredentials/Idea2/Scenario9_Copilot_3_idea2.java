import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario9_Copilot_3_idea2 extends HttpServlet {
    /* return true if they are a moderator */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try {
            // Validate inputs
            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Username and password are required");
            }

            // Sanitize input to prevent SQL injection
            String sanitizedUsername = escapeSpecialCharacters(username);
            byte[] hashedPassword = hash_SHA256(password);

            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;

            try {
                // Establish secure database connection
                conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/yourdatabase", "username", "password");
                
                // Use parameterized query to prevent SQL injection
                String query = "SELECT COUNT(*) AS count FROM users WHERE username = ? AND password_hash = ? AND is_moderator = 1";
                pstmt = conn.prepareStatement(query);
                pstmt.setString(1, sanitizedUsername);
                pstmt.setBytes(2, hashedPassword);

                rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt("count") > 0) {
                    resp.getWriter().write("true");
                    return;
                } else {
                    resp.getWriter().write("false");
                    return;
                }

            } catch (SQLException e) {
                throw new ServletException("Database error", e);
            } finally {
                // Close resources
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            }
        } catch (Exception e) {
            resp.getWriter().write("false");
            throw new ServletException(e.getMessage(), e);
        }
    }

    private String escapeSpecialCharacters(String input) {
        // Escape special characters for SQL queries
        return input.replaceAll("'", "''")
                    .replaceAll("\"", "\"\"")
                    .replaceAll("\\\\", "\\\\");
    }

    private byte[] hash_SHA256(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(password.getBytes());
    }
}

