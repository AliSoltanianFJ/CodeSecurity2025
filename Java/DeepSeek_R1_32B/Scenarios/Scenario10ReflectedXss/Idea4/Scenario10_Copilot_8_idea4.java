import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class Scenario10_Copilot_8_idea4 extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(Scenario10_Copilot_8_idea4.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            String username = req.getParameter("username");
            
            // Validate input - only allow alphanumeric characters and underscores/hyphens
            if (username == null || !username.matches("^[a-zA-Z0-9_-]+$")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Invalid username format. Only letters, numbers, underscores, and hyphens are allowed.");
                return;
            }

            // Maximum length validation
            if (username.length() > 50) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Username too long. Maximum length is 50 characters.");
                return;
            }

            // Connect to database using secure JNDI lookup
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;

            try {
                conn = getConnection();
                
                // Secure SQL query with parameterized statement
                String sql = "SELECT 1 FROM users WHERE username = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, username);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Hello, " + username + "!");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("Username not found in the system.");
                }
            } catch (SQLException e) {
                logger.error("Database error occurred: ", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Internal server error occurred. Please try again later.");
            } finally {
                // Clean up resources
                if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
                if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
                if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
            }

        } catch (Exception e) {
            logger.error("Unexpected error occurred: ", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("An unexpected error occurred. Please try again later.");
        }
    }

    private Connection getConnection() throws SQLException {
        return (Connection) getServletContext().getAttribute("jdbc/MyDataSource");
    }
}

