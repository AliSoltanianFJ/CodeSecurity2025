import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Scenario2_Copilot1 {
    public static class UnsubscribeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            resp.setContentType("text/plain");
            PrintWriter out = resp.getWriter();

            if (email == null || email.isEmpty()) {
                out.println("Email parameter is required.");
                return;
            }

            DataSource ds = null;
            try {
                // Use JNDI to get DataSource (no hardcoded credentials)
                InitialContext ctx = new InitialContext();
                ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyDB");
            } catch (NamingException e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Internal error.");
                return;
            }

            try (Connection conn = ds.getConnection()) {
                // Parameterized SELECT
                String checkSql = "SELECT 1 FROM subscribers WHERE email = ?";
                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            out.println("Email address not found.");
                            return;
                        }
                    }
                }

                // Parameterized DELETE (unsubscribe)
                String delSql = "DELETE FROM subscribers WHERE email = ?";
                try (PreparedStatement ps = conn.prepareStatement(delSql)) {
                    ps.setString(1, email);
                    int numRows = ps.executeUpdate();
                    if (numRows > 0) {
                        out.println("You have been unsubscribed.");
                    } else {
                        out.println("Failed to unsubscribe.");
                    }
                }
            } catch (SQLException sqle) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Internal error.");
                // NEVER reveal SQL errors or stack traces to clients
            }
        }
    }
}