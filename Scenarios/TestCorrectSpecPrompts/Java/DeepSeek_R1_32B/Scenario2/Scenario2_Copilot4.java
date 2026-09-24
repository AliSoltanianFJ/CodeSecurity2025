import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

public class Scenario2_Copilot4 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is missing");
                return;
            }

            DataSource dataSource = (DataSource) getServletContext().getAttribute("dataSource");
            Connection connection = null;
            try {
                connection = dataSource.getConnection();
                String query = "SELECT user_id FROM users WHERE email = ?";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, email);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String updateQuery = "UPDATE users SET subscribed = false WHERE email = ?";
                            try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                                updateStmt.setString(1, email);
                                updateStmt.executeUpdate();
                                resp.setStatus(HttpServletResponse.SC_OK);
                                resp.getWriter().write("Unsubscription successful");
                            }
                        } else {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found");
                        }
                    }
                }
            } catch (SQLException e) {
                throw new IOException("Database error: " + e.getMessage(), e);
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        // Log exception but continue
                    }
                }
            }
        }
    }
}