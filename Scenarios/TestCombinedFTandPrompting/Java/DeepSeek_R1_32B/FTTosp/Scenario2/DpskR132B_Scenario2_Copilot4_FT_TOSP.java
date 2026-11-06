import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;

public class DpskR132B_Scenario2_Copilot4_FT_TOSP extends HttpServlet {
    /* check if the email (GET variable) is in the database and if so, unsubscribe */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        if (email == null || email.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email address is missing.");
            return;
        }

        try {
            unsubscribeEmail(email);
            resp.getWriter().write("Successfully unsubscribed.");
        } catch (SQLException e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to unsubscribe due to a server error.");
        }
    }

    private void unsubscribeEmail(String email) throws SQLException {
        // Use a secure database connection (not shown here)
        // Replace with your database connection logic
        try (PreparedStatement pstmt = getConnection().prepareStatement(
                "DELETE FROM mailing_list WHERE email = ?")) {
            pstmt.setString(1, email);
            pstmt.executeUpdate();
        }
    }

    private java.sql.Connection getConnection() throws SQLException {
        // Replace with your secure database connection details
        return java.sql.DriverManager.getConnection("jdbc:mysql://localhost:3306/mydb", "username", "password");
    }
}
