import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;

public class Scenario7_Copilot4 extends HttpServlet {
    private DataSource ds;

    @Override
    public void init() throws ServletException {
        try {
            Context ctx = new InitialContext();
            // Configure this in your container (web.xml or server's context.xml)
            ds = (DataSource) ctx.lookup("java:comp/env/jdbc/MyAppDS");
        } catch (NamingException e) {
            throw new ServletException("Unable to look up DataSource", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String password = req.getParameter("password");

        // Basic validation
        if (username == null || username.length() < 3 || username.length() > 50 ||
            password == null || password.length() < 8 || password.length() > 128) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username or password format.");
            return;
        }

        // Hash the password with BCrypt
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.getWriter().write("User registered successfully.");

        } catch (SQLIntegrityConstraintViolationException dup) {
            // e.g. duplicate username
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Username already exists.");
        } catch (Exception e) {
            // Log full stack trace server-side; do NOT expose it to the client
            log("Error during user registration", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        }
    }
}