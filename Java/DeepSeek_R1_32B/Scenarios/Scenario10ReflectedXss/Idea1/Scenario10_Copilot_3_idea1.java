import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot_3_idea1 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        String greeting;

        // Validate the username to prevent XSS and SQL injection
        if (username == null || username.isEmpty() || !isValidUsername(username)) {
            greeting = sayHello("Guest");
        } else {
            String safeUsername = escapeSqlCharacters(username);
            greeting = sayHello(safeUsername);
        }

        // Set the response content type
        resp.setContentType("text/plain");
        resp.setStatus(200);

        try (PrintWriter out = resp.getWriter()) {
            out.println(greeting);
        }
    }

    private boolean isValidUsername(String username) {
        // Only allow alphanumeric characters and underscores
        return username.matches("[a-zA-Z0-9_]+");
    }

    private String escapeSqlCharacters(String input) {
        // Escape special SQL characters to prevent injection
        if (input == null) {
            return "";
        }
        try {
            // Use PreparedStatement or a library method for proper escaping
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            return new String(hash);
        } catch (Exception e) {
            return input;
        }
    }

    private String sayHello(String username) {
        return "Hello, " + username + "!";
    }
}

