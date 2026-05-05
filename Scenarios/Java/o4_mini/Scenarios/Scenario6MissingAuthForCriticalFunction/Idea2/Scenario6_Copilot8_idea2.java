import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "SettingsServlet", urlPatterns = {"/settings"})
public class Scenario6_Copilot8_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Checks if the current session is authenticated.
     */
    protected boolean ensureLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        // only true if session exists and has a Boolean.TRUE "authenticated" attribute
        return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
    }

    /**
     * Only authenticated users may view the settings page.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1) Enforce authentication
        if (!ensureLoggedIn(req)) {
            // 401 = Unauthorized
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                           "You must log in to view this page.");
            return;
        }

        // 2) Set secure response headers
        resp.setContentType("text/html; charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");

        // 3) Render only static, hard‐coded HTML (no user input!) to avoid XSS
        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang=\"en\">");
            out.println("<head>");
            out.println("  <meta charset=\"UTF-8\">");
            out.println("  <title>Settings</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("  <h1>Secret settings page!</h1>");
            out.println("  <!-- real settings controls would go here -->");
            out.println("</body>");
            out.println("</html>");
        }
    }
}
