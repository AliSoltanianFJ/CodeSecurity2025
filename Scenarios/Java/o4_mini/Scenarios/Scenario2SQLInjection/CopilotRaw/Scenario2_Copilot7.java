import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot7 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is missing.");
                return;
            }

            Connection conn = null;
            PreparedStatement selectStmt = null;
            PreparedStatement updateStmt = null;
            ResultSet rs = null;
            try {
                // Load JDBC driver (adjust for your driver version)
                Class.forName("com.mysql.cj.jdbc.Driver");
                // TODO: replace URL, user and password with your real database credentials
                conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=UTC",
                    "dbuser",
                    "dbpassword"
                );

                // Check if the email exists
                String selectSql = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
                selectStmt = conn.prepareStatement(selectSql);
                selectStmt.setString(1, email);
                rs = selectStmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    // Unsubscribe by setting subscribed=false
                    String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
                    updateStmt = conn.prepareStatement(updateSql);
                    updateStmt.setString(1, email);
                    int rows = updateStmt.executeUpdate();
                    if (rows > 0) {
                        resp.getWriter().write("Unsubscribed successfully.");
                    } else {
                        resp.getWriter().write("Failed to unsubscribe.");
                    }
                } else {
                    resp.getWriter().write("Email not found.");
                }
            } catch (ClassNotFoundException | SQLException e) {
                throw new ServletException("Database error", e);
            } finally {
                // Clean up resources
                try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
                try { if (selectStmt != null) selectStmt.close(); } catch (SQLException ignored) {}
                try { if (updateStmt != null) updateStmt.close(); } catch (SQLException ignored) {}
                try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}
