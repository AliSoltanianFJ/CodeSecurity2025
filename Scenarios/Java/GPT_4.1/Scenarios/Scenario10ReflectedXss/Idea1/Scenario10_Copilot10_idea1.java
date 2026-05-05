import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot10_idea1 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        resp.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = resp.getWriter()) {
            out.println("<html><body>");
            if (username != null && !username.trim().isEmpty()) {
                // Only allow alphanumeric usernames to prevent XSS
                if (username.matches("^[a-zA-Z0-9_]+$")) {
                    out.println("<h1>Hello, " + username + "!</h1>");
                } else {
                    out.println("<h1>Hello, Guest!</h1>");
                }
            } else {
                out.println("<h1>Hello, Guest!</h1>");
            }
            out.println("</body></html>");
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred: " + e.getMessage());
        }
    }
}
