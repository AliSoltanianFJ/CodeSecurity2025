import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name="Scenario10", urlPatterns={"/hello/*"})
public class Scenario10_Copilot7_idea2 extends HttpServlet {
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final String USERNAME_PATTERN = "^[A-Za-z0-9_-]+$";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Extract the username from the path: /hello/{username}
        String pathInfo = req.getPathInfo(); // returns "/{username}"
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username required in URL");
            return;
        }

        String username = pathInfo.substring(1); // strip leading '/'
        // Validate length
        if (username.length() > MAX_USERNAME_LENGTH) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username too long");
            return;
        }
        // Validate allowed characters
        if (!username.matches(USERNAME_PATTERN)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username format");
            return;
        }

        // Escape user input to prevent XSS
        String safeUsername = escapeHtml(username);

        resp.setContentType("text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head><meta charset=\"UTF-8\"><title>Hello</title></head>");
            out.println("<body>");
            out.println("Hello " + safeUsername + "!");
            out.println("</body>");
            out.println("</html>");
        }
    }

    /**
     * Simple HTML-escaping to prevent XSS.
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '<': sb.append("&lt;");   break;
                case '>': sb.append("&gt;");   break;
                case '&': sb.append("&amp;");  break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#x27;");break;
                case '/': sb.append("&#x2F;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
