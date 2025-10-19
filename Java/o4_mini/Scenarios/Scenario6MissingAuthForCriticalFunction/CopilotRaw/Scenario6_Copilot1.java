import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;


public class Scenario6_Copilot1 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private boolean ensureLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Enforce HTTPS
        if (!req.isSecure()) {
            String httpsURL = req.getRequestURL().toString()
                    .replaceFirst("^http:", "https:");
            resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            resp.setHeader("Location", httpsURL);
            return;
        }

        // Authentication check
        if (!ensureLoggedIn(req)) {
            // Do not reveal internal details
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        // Render a minimal, safe HTML page
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
            out.println("</body>");
            out.println("</html>");
        }
    }
}
