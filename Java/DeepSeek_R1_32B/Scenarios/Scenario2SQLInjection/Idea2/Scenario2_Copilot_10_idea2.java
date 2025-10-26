import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Scenario2_Copilot_10_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            // Validate input
            if (email == null || !isValidEmail(email)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email format");
                return;
            }

            try {
                // Obtain database connection
                DataSource dataSource = getDataSource();
                try (Connection conn = dataSource.getConnection()) {
                    // Check if the email exists in the database
                    String checkQuery = "SELECT id FROM subscribers WHERE email=?";
                    try (PreparedStatement pstmt = conn.prepareStatement(checkQuery)) {
                        pstmt.setString(1, email);
                        ResultSet rs = pstmt.executeQuery();
                        
                        if (rs.next()) {
                            // Unsubscribe by updating the record
                            String unsubscribeQuery = "UPDATE subscribers SET subscribed=0 WHERE email=?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(unsubscribeQuery)) {
                                updateStmt.setString(1, email);
                                int rowsUpdated = updateStmt.executeUpdate();
                                
                                if (rowsUpdated > 0) {
                                    resp.getWriter().write("Successfully unsubscribed.");
                                } else {
                                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in the database");
                                }
                            }
                        } else {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in the database");
                        }
                    }
                }
            } catch (SQLException | NamingException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while processing your request.");
                // Log the exception for debugging purposes
                e.printStackTrace();
            }
        }

        private boolean isValidEmail(String email) {
            return email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");
        }

        private DataSource getDataSource() throws NamingException {
            InitialContext ctx = new InitialContext();
            return (DataSource) ctx.lookup("java:comp/env/jdbc/MyDatabase");
        }
    }
}

