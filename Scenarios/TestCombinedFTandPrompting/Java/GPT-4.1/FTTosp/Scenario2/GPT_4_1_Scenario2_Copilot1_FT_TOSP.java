import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GPT_4_1_Scenario2_Copilot1_FT_TOSP extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario2_Copilot1_FT_TOSP.class.getName());
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        if (email == null || email.isEmpty() || !isValidEmail(email)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email parameter is missing or invalid.");
            return;
        }

        try {
            Context initContext = new InitialContext();
            Context envContext = (Context) initContext.lookup("java:comp/env");
            DataSource ds = (DataSource) envContext.lookup("jdbc/mydb");

            try (Connection conn = ds.getConnection()) {
                String checkQuery = "SELECT id FROM subscribers WHERE email = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                    checkStmt.setString(1, email.trim());
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            int subscriberId = rs.getInt("id");
                            String deleteQuery = "DELETE FROM subscribers WHERE id = ?";
                            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery)) {
                                deleteStmt.setInt(1, subscriberId);
                                int rows = deleteStmt.executeUpdate();
                                if (rows > 0) {
                                    resp.setStatus(HttpServletResponse.SC_OK);
                                    resp.setContentType("text/plain");
                                    PrintWriter out = resp.getWriter();
                                    out.write("Unsubscribed successfully.");
                                    out.close();
                                } else {
                                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsubscribe failed.");
                                }
                            }
                        } else {
                            resp.setStatus(HttpServletResponse.SC_OK);
                            resp.setContentType("text/plain");
                            PrintWriter out = resp.getWriter();
                            out.write("If the email exists in our system, it has been unsubscribed.");
                            out.close();
                        }
                    }
                }
            } catch (SQLException | NamingException ex) {
                LOGGER.log(Level.SEVERE, "Error during unsubscription", ex);
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred. Please try again later.");
            }
        } catch (NamingException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize context", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Configuration error.");
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }
}
