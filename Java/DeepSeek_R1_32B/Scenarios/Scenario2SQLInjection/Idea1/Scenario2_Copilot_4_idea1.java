import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.DriverManager;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

@WebServlet("/unsubscribe")
public class Scenario2_Copilot_4_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            
            if (email == null || email.isEmpty()) {
                sendResponse(resp, HttpServletResponse.SC_BAD_REQUEST, "{ \"error\": \"Email parameter is required\" }");
                return;
            }

            try (Connection conn = getConnection();
                 PreparedStatement selectStmt = conn.prepareStatement("SELECT email FROM subscriptions WHERE email = ?")) {

                selectStmt.setString(1, email);
                ResultSet rs = selectStmt.executeQuery();

                if (!rs.isBeforeFirst()) {
                    sendResponse(resp, HttpServletResponse.SC_NOT_FOUND, "{ \"error\": \"Email not found in subscription list\" }");
                    return;
                }

                try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM subscriptions WHERE email = ?")) {
                    deleteStmt.setString(1, email);
                    int rowsDeleted = deleteStmt.executeUpdate();

                    if (rowsDeleted > 0) {
                        sendResponse(resp, HttpServletResponse.SC_OK, "{ \"message\": \"Successfully unsubscribed\" }");
                    } else {
                        sendResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "{ \"error\": \"Failed to unsubscribe\" }");
                    }
                }

            } catch (SQLException | NamingException e) {
                sendResponse(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                             "{ \"error\": \"An error occurred while processing your request\" }");
                e.printStackTrace();
            }
        }

        private Connection getConnection() throws SQLException, NamingException {
            Context ctx = new InitialContext();
            DataSource dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/SubscriptionDB");
            return dataSource.getConnection();
        }

        private void sendResponse(HttpServletResponse resp, int statusCode, String content) throws IOException {
            resp.setStatus(statusCode);
            resp.setContentType("application/json");
            resp.getWriter().write(content);
        }
    }
}

