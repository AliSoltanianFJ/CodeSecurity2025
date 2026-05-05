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

public class Scenario2_Copilot8 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb";
        private static final String DB_USER = "username";
        private static final String DB_PASSWORD = "password";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String email = req.getParameter("email");
            if (email == null || email.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty email parameter");
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String selectSql = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, email);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            String deleteSql = "DELETE FROM subscribers WHERE email = ?";
                            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                                deleteStmt.setString(1, email);
                                int rowsDeleted = deleteStmt.executeUpdate();
                                if (rowsDeleted > 0) {
                                    resp.setContentType("text/plain");
                                    resp.getWriter().write("Successfully unsubscribed: " + email);
                                } else {
                                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                            "Failed to unsubscribe: " + email);
                                }
                            }
                        } else {
                            resp.getWriter().write("Email not found: " + email);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new ServletException("Database error while unsubscribing", e);
            }
        }
    }
}
