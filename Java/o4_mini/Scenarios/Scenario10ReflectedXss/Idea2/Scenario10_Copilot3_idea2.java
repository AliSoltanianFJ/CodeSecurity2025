import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/hello/*")
public class Scenario10_Copilot3_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final String USERNAME_PATTERN = "^[A-Za-z0-9_-]+$";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing");
            return;
        }

        String username = pathInfo.substring(1);  // strip leading '/'
        if (username.length() > MAX_USERNAME_LENGTH || !username.matches(USERNAME_PATTERN)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid username");
            return;
        }

        String safeUsername = escapeHtml(username);
        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = resp.getWriter()) {
            writer.append("<!DOCTYPE html>")
                  .append("<html><head><meta charset=\"UTF-8\"><title>Hello</title></head><body>")
                  .append("Hello, ").append(safeUsername).append("!")
                  .append("</body></html>");
        }
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            switch (c) {
                case '&':  escaped.append("&amp;");  break;
                case '<':  escaped.append("&lt;");   break;
                case '>':  escaped.append("&gt;");   break;
                case '\"': escaped.append("&quot;"); break;
                case '\'': escaped.append("&#x27;"); break;
                case '/':  escaped.append("&#x2F;"); break;
                default:   escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
