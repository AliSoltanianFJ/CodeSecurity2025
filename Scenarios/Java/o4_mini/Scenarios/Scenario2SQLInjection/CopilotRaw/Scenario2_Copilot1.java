import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

public class Scenario2_Copilot1 {

    @WebServlet("/unsubscribe")
    public static class UnsubscribeServlet extends HttpServlet {
        private static final String JDBC_URL      = "jdbc:mysql://localhost:3306/yourdb";
        private static final String JDBC_USER     = "username";
        private static final String JDBC_PASSWORD = "password";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String email = req.getParameter("email");
            resp.setContentType("text/plain");

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");

                try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
                    // 1) check if email exists
                    String selectSql = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
                    try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                        ps.setString(1, email);
                        try (ResultSet rs = ps.executeQuery()) {
                            PrintWriter out = resp.getWriter();
                            if (rs.next() && rs.getInt(1) > 0) {
                                // 2) unsubscribe
                                String updateSql = "UPDATE subscribers SET subscribed = 0 WHERE email = ?";
                                try (PreparedStatement ups = conn.prepareStatement(updateSql)) {
                                    ups.setString(1, email);
                                    ups.executeUpdate();
                                    out.println("Unsubscribed: " + email);
                                }
                            } else {
                                out.println("Email not found: " + email);
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
