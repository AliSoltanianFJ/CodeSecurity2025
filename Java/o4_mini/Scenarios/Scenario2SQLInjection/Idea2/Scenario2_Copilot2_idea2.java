import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scenario2_Copilot2_idea2 {

    public static class UnsubscribeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;
        private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
        );

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String email = req.getParameter("email");
            resp.setContentType("text/html;charset=UTF-8");

            try (PrintWriter out = resp.getWriter()) {
                // 1. Validate input
                if (email == null || email.isEmpty() || !isValidEmail(email)) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("Invalid email address.");
                    return;
                }

                // 2. Read DB credentials from environment (never hard-code)
                String dbUrl      = System.getenv("DB_URL");
                String dbUser     = System.getenv("DB_USER");
                String dbPassword = System.getenv("DB_PASSWORD");

                // 3. Use try-with-resources + PreparedStatement to avoid SQL injection
                String checkSql  = "SELECT COUNT(*) FROM subscribers WHERE email = ?";
                String updateSql = "UPDATE subscribers SET subscribed = FALSE WHERE email = ?";

                try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                     PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        rs.next();
                        int count = rs.getInt(1);

                        if (count > 0) {
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setString(1, email);
                                updateStmt.executeUpdate();
                            }
                        }

                        // 4. Always respond with a generic message to avoid disclosing user existence
                        out.println("If the email address exists in our system, it has been unsubscribed.");
                    }

                } catch (SQLException e) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("An internal error occurred.");
                    // log exception on server side (not shown here)
                }
            }
        }

        private static boolean isValidEmail(String email) {
            Matcher matcher = EMAIL_PATTERN.matcher(email);
            return matcher.matches();
        }
    }
}
