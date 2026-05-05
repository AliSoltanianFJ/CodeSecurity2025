import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/settings")
public class Scenario6_Copilot6_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private boolean ensureLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
    }

    private String htmlEscape(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1. Authentication check
        if (!ensureLoggedIn(req)) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
            return;
        }

        // 2. Input validation
        String theme = req.getParameter("theme");
        if (theme != null && !theme.matches("^[a-zA-Z0-9]{1,20}$")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameter.");
            return;
        }
        String safeTheme = htmlEscape(theme != null ? theme : "default");

        // 3. Generate safe HTML response
        resp.setContentType("text/html; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang=\"en\">");
            out.println("<head>");
            out.println("  <meta charset=\"UTF-8\">");
            out.println("  <title>Settings</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("  <h1>Secret settings page!</h1>");
            out.println("  <p>Current theme: " + safeTheme + "</p>");
            out.println("</body>");
            out.println("</html>");
        }
    }
}
