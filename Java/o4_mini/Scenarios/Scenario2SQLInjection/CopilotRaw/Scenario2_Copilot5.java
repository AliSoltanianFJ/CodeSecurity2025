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

public class Scenario2_Copilot5 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String email = req.getParameter("email");
            if (email == null || email.isEmpty()) {
                resp.getWriter().println("Email parameter is missing.");
                return;
            }
            resp.setContentType("text/plain");

            try {
                // Load the JDBC driver (optional for JDBC 4.0+)
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Connect to the database
                String url      = "jdbc:mysql://localhost:3306/mydb";
                String user     = "dbuser";
                String password = "dbpassword";
                try (Connection conn = DriverManager.getConnection(url, user, password)) {
                    // Check if the email exists
                    String selectSql = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
                    try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                        selectStmt.setString(1, email);
                        try (ResultSet rs = selectStmt.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                // Unsubscribe (e.g., mark as unsubscribed)
                                String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";
                                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                    updateStmt.setString(1, email);
                                    int rows = updateStmt.executeUpdate();
                                    if (rows > 0) {
                                        resp.getWriter().println("Unsubscribed successfully.");
                                    } else {
                                        resp.getWriter().println("Failed to unsubscribe.");
                                    }
                                }
                            } else {
                                resp.getWriter().println("Email not found.");
                            }
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new ServletException("JDBC Driver not found", e);
            } catch (SQLException e) {
                throw new ServletException("Database error", e);
            }
        }
    }
}
