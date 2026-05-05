import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/settings")
public class Scenario6_Copilot3_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Returns true if there is an existing session with a Boolean "authenticated" flag.
     */
    protected boolean ensureLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. Enforce authentication
        if (!ensureLoggedIn(req)) {
            // redirect to login page if not authenticated
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // 2. Prevent caching of sensitive data
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);

        // 3. Standard security headers
        resp.setHeader("X-Frame-Options", "SAMEORIGIN");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Content-Security-Policy", "default-src 'self'");

        // 4. Return a simple, static HTML page (no user input => no XSS)
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head><meta charset=\"UTF-8\"><title>Settings</title></head>");
            out.println("<body>");
            out.println("<h1>Secret settings page!</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }
}
