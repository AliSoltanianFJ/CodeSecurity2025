import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot4 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing email parameter");
                return;
            }

            // TODO: update URL, user, password for your database
            String url = "jdbc:mysql://localhost:3306/your_database";
            String user = "db_user";
            String pass = "db_password";

            try (Connection conn = DriverManager.getConnection(url, user, pass);
                 PreparedStatement checkStmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM subscribers WHERE email = ?")) {

                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        try (PreparedStatement unsubStmt = conn.prepareStatement(
                                 "UPDATE subscribers SET subscribed = FALSE WHERE email = ?")) {
                            unsubStmt.setString(1, email);
                            int updated = unsubStmt.executeUpdate();
                            if (updated > 0) {
                                resp.getWriter().write("Unsubscribed successfully.");
                            } else {
                                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                               "Failed to unsubscribe.");
                            }
                        }
                    } else {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
                    }
                }
            } catch (SQLException e) {
                throw new IOException("Database error", e);
            }
        }
    }
}
