import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

public class Scenario2_Copilot9 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is missing");
                return;
            }

            DataSource dataSource = (DataSource) getServletContext().getAttribute("dataSource");
            if (dataSource == null) {
                throw new RuntimeException("DataSource not configured");
            }

            PreparedStatement statement = null;
            try {
                Connection connection = dataSource.getConnection();
                try {
                    String query = "SELECT COUNT(*) FROM users WHERE email = ?";
                    statement = connection.prepareStatement(query);
                    statement.setString(1, email);
                    ResultSet resultSet = statement.executeQuery();
                    try {
                        if (resultSet.next()) {
                            if (resultSet.getInt(1) > 0) {
                                // Perform unsubscribed action
                                System.out.println("Unsubscribing user with email: " + email);
                                resp.sendRedirect("unsubscribe Confirmation");
                            } else {
                                System.out.println("Email " + email + " not found");
                                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found");
                            }
                        }
                    } finally {
                        resultSet.close();
                    }
                } finally {
                    if (statement != null) {
                        statement.close();
                    }
                    connection.close();
                }
            } catch (SQLException e) {
                throw new IOException("Database error", e);
            }
        }
    }
}
