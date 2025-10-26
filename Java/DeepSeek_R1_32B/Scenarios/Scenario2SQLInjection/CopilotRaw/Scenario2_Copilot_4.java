import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;

public class Scenario2_Copilot_4 {
    
    public class DBUtils {
        private static final String URL = "jdbc:mysql://localhost:3306/your_database";
        private static final String USER = "your_username";
        private static final String PASSWORD = "your_password";

        // Optional: load the JDBC driver (depends on your setup)
        static {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("MySQL JDBC Driver not found.", e);
            }
        }

        public static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        }
    }

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Email parameter is required.");
                return;
            }
            
            try (Connection conn = DBUtils.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT id FROM subscribers WHERE email = ?")) {
                
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) { // Email exists in database
                    int userId = rs.getInt("id");
                    
                    try (PreparedStatement deleteStmt = conn.prepareStatement(
                            "DELETE FROM subscribers WHERE id = ?")) {
                        deleteStmt.setInt(1, userId);
                        int rowsDeleted = deleteStmt.executeUpdate();
                        
                        if (rowsDeleted == 1) {
                            resp.setStatus(HttpServletResponse.SC_OK);
                            resp.getWriter().write("Successfully unsubscribed.");
                        } else {
                            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            resp.getWriter().write("Error unsubscribing user.");
                        }
                    }
                } else { // Email not found
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("No subscription found for this email.");
                }
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Database error occurred: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

