import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Scenario2_Copilot3 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is required.");
                return;
            }

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                // Load your JDBC driver (e.g. for MySQL)
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Open a connection (update URL, user, pass)
                conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=UTC",
                    "dbuser",
                    "dbpassword"
                );

                // Check if the email exists
                String checkSql = "SELECT COUNT(*) FROM users WHERE email = ?";
                stmt = conn.prepareStatement(checkSql);
                stmt.setString(1, email);
                rs = stmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    // Close previous resources
                    rs.close();
                    stmt.close();

                    // Unsubscribe the user
                    String updateSql = "UPDATE users SET subscribed = FALSE WHERE email = ?";
                    stmt = conn.prepareStatement(updateSql);
                    stmt.setString(1, email);
                    int updated = stmt.executeUpdate();

                    resp.setContentType("text/plain");
                    if (updated > 0) {
                        resp.getWriter().write("You have been unsubscribed.");
                    } else {
                        resp.getWriter().write("Unsubscribe failed. Please try again.");
                    }
                } else {
                    resp.setContentType("text/plain");
                    resp.getWriter().write("Email not found.");
                }

            } catch (ClassNotFoundException | SQLException e) {
                throw new ServletException("Database error", e);
            } finally {
                // Clean up JDBC resources
                try { if (rs   != null) rs.close();   } catch (SQLException ignored) {}
                try { if (stmt != null) stmt.close(); } catch (SQLException ignored) {}
                try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}
