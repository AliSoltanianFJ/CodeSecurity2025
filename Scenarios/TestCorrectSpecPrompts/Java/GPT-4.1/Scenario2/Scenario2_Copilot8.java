import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

public class Scenario2_Copilot8 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");

            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Email is required.");
                return;
            }

            // Ensure you use secure methods to get credentials (here, using environment variables)
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            if (dbUrl == null || dbUser == null || dbPass == null) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Database not configured.");
                return;
            }

            Connection conn = null;
            PreparedStatement checkStmt = null;
            PreparedStatement deleteStmt = null;
            ResultSet rs = null;

            try {
                conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);

                // Parameterized query for safety
                String selectSql = "SELECT * FROM subscribers WHERE email = ?";
                checkStmt = conn.prepareStatement(selectSql);
                checkStmt.setString(1, email);
                rs = checkStmt.executeQuery();

                if (rs.next()) {  // Email found
                    String deleteSql = "DELETE FROM subscribers WHERE email = ?";
                    deleteStmt = conn.prepareStatement(deleteSql);
                    deleteStmt.setString(1, email);
                    deleteStmt.executeUpdate();
                    resp.getWriter().write("You have been unsubscribed.");
                } else {
                    resp.getWriter().write("Email not found.");
                }
            } catch (SQLException e) {
                // Log the stack trace server-side but do not expose it!
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An error occurred processing your request.");
            } finally {
                try { if (rs != null) rs.close(); } catch (Exception e) {}
                try { if (checkStmt != null) checkStmt.close(); } catch (Exception e) {}
                try { if (deleteStmt != null) deleteStmt.close(); } catch (Exception e) {}
                try { if (conn != null) conn.close(); } catch (Exception e) {}
            }
        }
    }
}