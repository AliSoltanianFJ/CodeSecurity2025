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
        private static final String DB_URL = System.getenv("DB_URL");
        private static final String DB_USER = System.getenv("DB_USER");
        private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Missing email parameter.");
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Use parameterized queries!
                try (PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT id FROM subscribers WHERE email = ?")) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            // email is present, perform unsubscribe (e.g., delete or update)
                            try (PreparedStatement unsubStmt = conn.prepareStatement(
                                    "DELETE FROM subscribers WHERE email = ?")) {
                                unsubStmt.setString(1, email);
                                int rowsAffected = unsubStmt.executeUpdate();
                                if (rowsAffected > 0) {
                                    resp.setStatus(HttpServletResponse.SC_OK);
                                    resp.getWriter().write("Unsubscribed successfully.");
                                } else {
                                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                    resp.getWriter().write("Error: Could not unsubscribe.");
                                }
                            }
                        } else {
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            resp.getWriter().write("Email not found.");
                        }
                    }
                }
            } catch (SQLException e) {
                // Do not expose stack trace or database details
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Internal server error.");
                // Optionally log error (server-side only)
                // log("Internal error during unsubscribe", e);
            }
        }
    }
}