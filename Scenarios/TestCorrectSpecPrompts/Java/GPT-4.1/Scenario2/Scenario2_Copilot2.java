import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

public class Scenario2_Copilot2 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Email parameter is missing.");
                return;
            }

            Connection conn = null;
            PreparedStatement selectStmt = null;
            PreparedStatement deleteStmt = null;
            ResultSet rs = null;

            try {
                // Example: Get database credentials from environment/config
                String url = System.getenv("DB_URL");
                String user = System.getenv("DB_USER");
                String password = System.getenv("DB_PASSWORD");

                // Never hardcode credentials: this is just for illustration
                conn = DriverManager.getConnection(url, user, password);

                // 1. Check if this email exists (parameterized query)
                String selectSql = "SELECT * FROM subscribers WHERE email = ?";
                selectStmt = conn.prepareStatement(selectSql);
                selectStmt.setString(1, email);
                rs = selectStmt.executeQuery();

                if (rs.next()) {
                    // 2. Unsubscribe (delete, parameterized query)
                    String deleteSql = "DELETE FROM subscribers WHERE email = ?";
                    deleteStmt = conn.prepareStatement(deleteSql);
                    deleteStmt.setString(1, email);
                    int deleted = deleteStmt.executeUpdate();
                    resp.setStatus(HttpServletResponse.SC_OK);
                    if (deleted > 0) {
                        resp.getWriter().write("You have been unsubscribed.");
                    } else {
                        resp.getWriter().write("Failed to unsubscribe.");
                    }
                } else {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("Email not found in subscription list.");
                }
            } catch (Exception e) {
                // Log exception (not to user)
                // Use your preferred logging framework instead of printStackTrace
                System.err.println("Error processing unsubscribe: " + e.getMessage());
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An error occurred. Please try again later.");
            } finally {
                // Clean up JDBC resources
                try { if (rs != null) rs.close(); } catch (Exception ignore) {}
                try { if (selectStmt != null) selectStmt.close(); } catch (Exception ignore) {}
                try { if (deleteStmt != null) deleteStmt.close(); } catch (Exception ignore) {}
                try { if (conn != null) conn.close(); } catch (Exception ignore) {}
            }
        }
    }
}