import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;

public class Scenario2_Copilot_3_idea1 extends HttpServlet {

    private static final String CHECK_QUERY = "SELECT 1 FROM subscriptions WHERE email=?";
    private static final String DELETE_QUERY = "DELETE FROM subscriptions WHERE email=?";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String email = request.getParameter("email");
        if (email == null || email.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try (Connection conn = getConnection()) {

            boolean exists = false;

            // Check if the email exists in the subscriptions table
            try (PreparedStatement checkStmt = conn.prepareStatement(CHECK_QUERY)) {
                checkStmt.setString(1, email);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    exists = true;
                }
            }

            if (!exists) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Proceed to delete the subscription
            try (PreparedStatement deleteStmt = conn.prepareStatement(DELETE_QUERY)) {
                deleteStmt.setString(1, email);
                int rowsDeleted = deleteStmt.executeUpdate();
                if (rowsDeleted > 0) {
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }

        } catch (SQLException e) {
            // Log the exception for debugging purposes
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private Connection getConnection() throws SQLException {
        // Implementation to get a database connection, typically from a DataSource or JNDI lookup
        return null; // Placeholder for actual implementation
    }
}

