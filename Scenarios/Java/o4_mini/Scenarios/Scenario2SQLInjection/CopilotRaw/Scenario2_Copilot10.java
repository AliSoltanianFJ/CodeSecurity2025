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

public class Scenario2_Copilot10 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb";
        private static final String DB_USER = "your_db_username";
        private static final String DB_PASSWORD = "your_db_password";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            resp.setContentType("text/plain");
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty email parameter");
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Check if the email exists
                String checkSql = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
                try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                    psCheck.setString(1, email);
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            // Unsubscribe (delete or flag as unsubscribed)
                            String deleteSql = "DELETE FROM subscribers WHERE email = ?";
                            try (PreparedStatement psDel = conn.prepareStatement(deleteSql)) {
                                psDel.setString(1, email);
                                int rowsAffected = psDel.executeUpdate();
                                if (rowsAffected > 0) {
                                    resp.getWriter().println("Successfully unsubscribed: " + email);
                                } else {
                                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                            "Failed to unsubscribe");
                                }
                            }
                        } else {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found");
                        }
                    }
                }
            } catch (SQLException e) {
                throw new ServletException("Database error while processing unsubscribe", e);
            }
        }
    }
}
