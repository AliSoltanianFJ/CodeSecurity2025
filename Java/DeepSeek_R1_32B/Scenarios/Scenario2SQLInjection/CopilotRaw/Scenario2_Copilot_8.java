import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario2_Copilot_8 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Email is required.");
                return;
            }

            Context ctx;
            DataSource ds = null;
            Connection conn = null;
            PreparedStatement stmtSelect = null;
            ResultSet rs = null;
            try {
                ctx = new InitialContext();
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDataSource");
                conn = ds.getConnection();

                String selectQuery = "SELECT email FROM subscribers WHERE email=?";
                stmtSelect = conn.prepareStatement(selectQuery);
                stmtSelect.setString(1, email);
                rs = stmtSelect.executeQuery();

                if (rs.next()) {
                    // Email found, proceed to unsubscribe
                    PreparedStatement stmtDelete = null;
                    String deleteQuery = "DELETE FROM subscribers WHERE email=?";
                    stmtDelete = conn.prepareStatement(deleteQuery);
                    stmtDelete.setString(1, email);
                    int rowsDeleted = stmtDelete.executeUpdate();
                    
                    if (rowsDeleted > 0) {
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().write("Successfully unsubscribed.");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().write("Email not found in the database.");
                    }
                } else {
                    // Email does not exist
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("Email not found in the database.");
                }
            } catch (SQLException | javax.naming.NamingException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error processing unsubscribe request.");
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmtSelect != null) stmtSelect.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

