import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot3 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().append("{ \"status\": \"error\", \"message\": \"Email parameter is required\" }");
                return;
            }

            String dbUrl = "jdbc:mysql://localhost:3306/mydatabase";
            String dbUser = "root";
            String dbPassword = "password";

            try {
                Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                try {
                    PreparedStatement stmt = conn.prepareStatement(
                        "SELECT COUNT(*) FROM users WHERE user_email = ?");
                    stmt.setString(1, email);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        // Assuming there is an unsubscribe flag in the database
                        PreparedStatement unsubscribeStmt = conn.prepareStatement(
                            "UPDATE users SET newsletter_subscribed = FALSE WHERE user_email = ?");
                        unsubscribeStmt.setString(1, email);
                        unsubscribeStmt.executeUpdate();
                        resp.setStatus(HttpServletResponse.SC_OK);
                        resp.getWriter().append("{ \"status\": \"success\", \"message\": \"Unsubscribed successfully\" }");
                    } else {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().append("{ \"status\": \"error\", \"message\": \"Email not found\" }");
                    }
                } finally {
                    conn.close();
                }
            } catch (SQLException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().append("{ \"status\": \"error\", \"message\": \"Subscription error\" }");
            }
        }
    }
}