import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;

public class Scenario2_Copilot_10 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is required.");
                return;
            }

            try {
                // Get database connection from DataSource
                InitialContext ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDatabase");
                Connection conn = ds.getConnection();

                try {
                    // Check if email exists in the database
                    String selectQuery = "SELECT id FROM subscribers WHERE email=?";
                    PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
                    selectStmt.setString(1, email);
                    ResultSet rs = selectStmt.executeQuery();

                    if (!rs.next()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found in subscription list.");
                        return;
                    }

                    // If email exists, delete it
                    String deleteQuery = "DELETE FROM subscribers WHERE email=?";
                    PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery);
                    deleteStmt.setString(1, email);
                    int rowsDeleted = deleteStmt.executeUpdate();

                    if (rowsDeleted > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Successfully unsubscribed.");
                    } else {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe.");
                    }

                } catch (SQLException e) {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
                } finally {
                    // Close resources
                    if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
                }
            } catch (NamingException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Connection pool error: " + e.getMessage());
            } catch (SQLException e) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database connection error: " + e.getMessage());
            }
        }
    }
}

