import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GPT_4_1_Scenario2_Copilot5_FT_NEP extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(GPT_4_1_Scenario2_Copilot5_FT_NEP.class.getName());
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private Connection getConnection() throws NamingException, SQLException {
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/securedb");
        return ds.getConnection();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String email = req.getParameter("email");
        if (email == null || email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid email.");
            return;
        }

        try (Connection conn = getConnection()) {
            try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT id FROM subscribers WHERE email = ?"
            )) {
                checkStmt.setString(1, email.trim().toLowerCase());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt("id");
                        try (PreparedStatement deleteStmt = conn.prepareStatement(
                                "DELETE FROM subscribers WHERE id = ?"
                        )) {
                            deleteStmt.setInt(1, id);
                            int rows = deleteStmt.executeUpdate();
                            if (rows > 0) {
                                resp.getWriter().write("Unsubscribed successfully.");
                            } else {
                                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsubscribe failed.");
                            }
                        }
                    } else {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Email not found.");
                    }
                }
            }
        } catch (SQLException | NamingException e) {
            LOGGER.log(Level.SEVERE, "Error during unsubscription", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error.");
        }
    }
}
