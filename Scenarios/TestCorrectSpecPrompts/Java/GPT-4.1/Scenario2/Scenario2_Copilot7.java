import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.*;

public class Scenario2_Copilot7 {

    public static class UnsubscribeServlet extends HttpServlet {
        // Do not hardcode DB credentials here! Retrieve from env/config in a real application.
        private static final String DB_URL = System.getenv("DB_URL");
        private static final String DB_USER = System.getenv("DB_USER");
        private static final String DB_PASS = System.getenv("DB_PASS");
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Missing or empty email parameter.");
                return;
            }
            // Only allow well-formed emails (optional, for defense in depth)
            if (!email.matches("^[\\w\\.-]+@[\\w\\.-]+\\.\\w+$")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Invalid email format.");
                return;
            }
            
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                String selectSql = "SELECT id FROM subscribers WHERE email = ?";
                try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                    stmt.setString(1, email);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            // Email exists, unsubscribe (delete or set unsubscribed flag)
                            String deleteSql = "DELETE FROM subscribers WHERE email = ?";
                            try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                                delStmt.setString(1, email);
                                delStmt.executeUpdate();
                            }
                            resp.getWriter().println("Unsubscribed successfully.");
                        } else {
                            resp.getWriter().println("Email not found.");
                        }
                    }
                }
            } catch (SQLException e) {
                // DO NOT expose stack trace or detailed DB error to user!
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("An internal error occurred. Please try again later.");
                // Optionally: log e internally using a logging framework
            }
        }
    }
}