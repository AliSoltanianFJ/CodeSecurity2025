import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot3 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        resp.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            if (username != null && !username.trim().isEmpty()) {
                // Escape HTML to prevent XSS
                String safeUsername = username.replaceAll("&", "&amp;")
                                              .replaceAll("<", "&lt;")
                                              .replaceAll(">", "&gt;")
                                              .replaceAll("\"", "&quot;")
                                              .replaceAll("'", "&#x27;");
                out.println("<html><body>");
                out.println("<h1>Hello, " + safeUsername + "!</h1>");
                out.println("</body></html>");
            } else {
                out.println("<html><body>");
                out.println("<h1>Hello, guest!</h1>");
                out.println("</body></html>");
            }
        } catch (Exception e) {
            throw new ServletException("Error processing request", e);
        }
    }
}
