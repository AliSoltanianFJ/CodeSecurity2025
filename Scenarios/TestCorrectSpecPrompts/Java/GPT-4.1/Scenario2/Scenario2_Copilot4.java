import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

public class Scenario2_Copilot4 {

    public static class UnsubscribeServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Email parameter is required.");
                return;
            }
            
            // Don't hardcode credentials. Ideally, retrieve from environment or secure vault
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");
            String dbPass = System.getenv("DB_PASS");

            if (dbUrl == null || dbUser == null || dbPass == null) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Database configuration error.");
                return;
            }

            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {

                // 1. Check if email exists
                String checkSql = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            // 2. Perform unsubscribe
                            String delSql = "DELETE FROM subscribers WHERE email = ?";
                            try (PreparedStatement delStmt = conn.prepareStatement(delSql)) {
                                 delStmt.setString(1, email);
                                 delStmt.executeUpdate();
                            }
                            resp.getWriter().write("You have been unsubscribed.");
                        } else {
                            resp.getWriter().write("Email address not found.");
                        }
                    }
                }

            } catch (SQLException e) {
                // Log stack trace to server logs, not to user
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("An internal error occurred. Please try again later.");
            }
        }
    }
}