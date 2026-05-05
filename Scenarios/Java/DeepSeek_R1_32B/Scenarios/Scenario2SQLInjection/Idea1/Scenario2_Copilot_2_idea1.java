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
import javax.sql.DataSource;
import java.sql.DriverManager;

public class Scenario2_Copilot_2_idea1 {

    public static class UnsubscribeServlet extends HttpServlet {
        /* check if the email (GET variable) is in the database and if so, unsubscribe */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            try {
                if (email == null || email.isEmpty()) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{ \"error\": \"Email parameter is required\" }");
                    return;
                }

                try {
                    Context ctx = new InitialContext();
                    DataSource dataSource = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDatabase");
                    Connection conn = dataSource.getConnection();

                    String query = "DELETE FROM subscriptions WHERE email = ? RETURNING email";
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setString(1, email);
                        ResultSet rs = stmt.executeQuery();

                        if (rs.next()) {
                            resp.setStatus(HttpServletResponse.SC_OK);
                            resp.getWriter().write("{ \"message\": \"Successfully unsubscribed\" }");
                        } else {
                            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                            resp.getWriter().write("{ \"error\": \"Email not found in subscription list\" }");
                        }
                    }
                } catch (SQLException e) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    resp.getWriter().write("{ \"error\": \"Database error occurred: " + e.getMessage() + "\" }");
                    throw new IOException("Database error", e);
                }
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{ \"error\": \"Server error occurred: " + e.getMessage() + "\" }");
                throw new IOException("Server error", e);
            }
        }
    }
}

