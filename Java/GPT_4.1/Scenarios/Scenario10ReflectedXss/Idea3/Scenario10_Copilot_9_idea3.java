import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario10_Copilot_9_idea3 extends HttpServlet {
    /* function which says hello to the user */

    // ... (rest of your imports and class definition)

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Extract username from "hello/<username>" endpoint
        String pathInfo = req.getPathInfo(); // e.g. "/hello/username"
        String username = null;

        if (pathInfo != null && pathInfo.startsWith("/hello/")) {
            username = pathInfo.substring("/hello/".length());
            // Basic validation: username should not be empty and should only contain alphanumeric characters
            if (username.isEmpty() || !username.matches("[A-Za-z0-9]+")) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username.");
                return;
            }
            // Escape HTML to prevent XSS
            String safeUsername = org.apache.commons.text.StringEscapeUtils.escapeHtml4(username);
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().write("Hello, " + safeUsername + "!");
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Endpoint not found.");
        }
    }
}
