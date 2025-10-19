import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot2 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String email = req.getParameter("email");
            if (email == null || email.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is missing");
                return;
            }

            // JDBC connection settings (replace with your real values)
            String url = "jdbc:mysql://localhost:3306/yourdb";
            String user = "dbuser";
            String pass = "dbpassword";

            try {
                // Load MySQL driver (adjust classname for your driver version)
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new ServletException("JDBC Driver not found", e);
            }

            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                // Check if email exists
                String selectSQL = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
                try (PreparedStatement ps = conn.prepareStatement(selectSQL)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            // Unsubscribe by deleting the record
                            String deleteSQL = "DELETE FROM subscribers WHERE email = ?";
                            try (PreparedStatement psDelete = conn.prepareStatement(deleteSQL)) {
                                psDelete.setString(1, email);
                                psDelete.executeUpdate();
                                resp.setContentType("text/plain");
                                try (PrintWriter out = resp.getWriter()) {
                                    out.println("You have been unsubscribed.");
                                }
                            }
                        } else {
                            resp.setContentType("text/plain");
                            try (PrintWriter out = resp.getWriter()) {
                                out.println("Email not found.");
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                throw new ServletException("Database error", e);
            }
        }
    }
}
