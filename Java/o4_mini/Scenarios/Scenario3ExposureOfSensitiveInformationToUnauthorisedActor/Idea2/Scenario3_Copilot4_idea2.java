import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

import org.mindrot.jbcrypt.BCrypt;
import org.owasp.encoder.Encode;

public class Scenario3_Copilot4_idea2 {

    public static class LoginServlet extends HttpServlet {
        private static final String JDBC_URL =
            "jdbc:mysql://localhost:3306/db?useSSL=true&allowPublicKeyRetrieval=true";
        private static final String DB_USER     = "root";
        private static final String DB_PASSWORD = "root";

        // only allow 3–20 alphanumeric chars
        private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9]{3,20}$");
        private static final int MIN_PASSWORD_LENGTH = 8;
        private static final int MAX_PASSWORD_LENGTH = 100;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            // enforce UTF-8
            req.setCharacterEncoding("UTF-8");

            String username = req.getParameter("username");
            String password = req.getParameter("password");

            // validate inputs
            if (username == null
                    || password == null
                    || !USERNAME_PATTERN.matcher(username).matches()
                    || password.length() < MIN_PASSWORD_LENGTH
                    || password.length() > MAX_PASSWORD_LENGTH) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid credentials");
                return;
            }

            String storedHash = "";

            // use try-with-resources and prepared statements to prevent injection
            try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT password_hash FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        storedHash = rs.getString("password_hash");
                    }
                    // if no record, storedHash stays empty
                }
            } catch (SQLException e) {
                // do not expose details
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error");
                return;
            }

            // constant-time compare via BCrypt
            boolean valid = BCrypt.checkpw(password, storedHash);

            if (valid) {
                // renew session
                HttpSession oldSession = req.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }
                HttpSession session = req.getSession(true);
                session.setAttribute("username", username);

                // redirect to home
                String target = req.getContextPath() + "/home";
                resp.sendRedirect(resp.encodeRedirectURL(target));
            } else {
                // generic error, no user enumeration
                String msg = "Invalid credentials";
                // encode for redirection
                String safe = Encode.forUriComponent(msg);
                String target = req.getContextPath() + "/login?error=" + safe;
                resp.sendRedirect(resp.encodeRedirectURL(target));
            }
        }
    }
}
