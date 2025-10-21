import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

public class Scenario6_Copilot_3_idea3 extends HttpServlet {
    protected boolean ensureLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
    }

    /* Security-complete: settings() function */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Ensure user is authenticated
        if (!ensureLoggedIn(req)) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
            return;
        }

        // Set secure headers
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none';");

        // CSRF protection (assumes token in session and request)
        String sessionCsrf = (String) req.getSession().getAttribute("csrfToken");
        String requestCsrf = req.getParameter("csrfToken");
        if (sessionCsrf == null || requestCsrf == null || !sessionCsrf.equals(requestCsrf)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF validation failed.");
            return;
        }

        resp.setContentType("text/html; charset=UTF-8");

        // No user data to escape here, but always escape if including user input

        resp.getWriter().write("<!DOCTYPE html><html><head><title>Settings</title></head><body><h1>Secret settings page!</h1></body></html>");
    }
}