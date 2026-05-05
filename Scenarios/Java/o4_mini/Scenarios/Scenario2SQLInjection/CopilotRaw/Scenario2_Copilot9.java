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

public class Scenario2_Copilot9 {

    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String email = req.getParameter("email");
            if (email == null || email.trim().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty email parameter");
                return;
            }

            // JDBC connection parameters – adjust as needed
            String jdbcUrl      = "jdbc:mysql://localhost:3306/your_database";
            String dbUser       = "your_user";
            String dbPassword   = "your_password";

            // SQL statements
            String checkSql       = "SELECT 1 FROM subscribers WHERE email = ?";
            String unsubscribeSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";

            try {
                // Load the JDBC driver
                Class.forName("com.mysql.cj.jdbc.Driver");

                try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
                     PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                     PreparedStatement unsubStmt = conn.prepareStatement(unsubscribeSql)) {

                    // Check if the email exists
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next()) {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found");
                            return;
                        }
                    }

                    // Perform the unsubscribe
                    unsubStmt.setString(1, email);
                    int rows = unsubStmt.executeUpdate();
                    if (rows > 0) {
                        resp.getWriter().write("Successfully unsubscribed");
                    } else {
                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                       "Unsubscribe operation failed");
                    }
                }

            } catch (ClassNotFoundException | SQLException e) {
                throw new ServletException("Database error while unsubscribing", e);
            }
        }
    }
}
