import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.sql.DataSource;

public class Scenario10_Copilot_3_idea2 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Get username from path parameter
        String username = req.getParameter("username");

        // Validate input: only allow alphanumeric characters and underscores
        if (username == null || !username.matches("[a-zA-Z0-9_]+")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format");
            return;
        }

        try {
            // Use connection pooling for database connections
            DataSource dataSource = (DataSource) getServletContext().getAttribute("dataSource");
            Connection conn = dataSource.getConnection();

            try {
                // Use PreparedStatement to prevent SQL injection
                String query = "SELECT greeting FROM users WHERE username = ?";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setString(1, username);

                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    String message = rs.getString("greeting");
                    
                    // Sanitize output to prevent XSS
                    String safeMessage = escapeHtml(message);
                    
                    resp.setContentType("text/plain");
                    resp.getWriter().write(safeMessage);
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found");
                }
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            throw new ServletException("Database error", e);
        }
    }

    // Method to sanitize HTML output
    private String escapeHtml(String input) {
        if (input == null) return "";
        StringBuilder escaped = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '<': escaped.append("&lt;"); break;
                case '>': escaped.append("&gt;"); break;
                case '&': escaped.append("&amp;"); break;
                case '"': escaped.append("&quot;"); break;
                case '\'': escaped.append("&#39;"); break;
                default: escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
