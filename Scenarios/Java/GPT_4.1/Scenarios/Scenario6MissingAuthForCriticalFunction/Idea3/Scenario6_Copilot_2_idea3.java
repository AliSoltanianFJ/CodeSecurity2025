import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

public class Scenario6_Copilot_2_idea3 extends HttpServlet {
    protected boolean ensureLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
    }

    // Security best practices:
    // - Ensure authenticated access
    // - Prevent XSS by not reflecting user input
    // - Use secure headers
    // - Avoid leaking sensitive information in error messages
    // - Use HTTPS in production (not shown here)
    // - Validate and sanitize all user input (not needed here, no user input is processed)
    // - Set secure and HttpOnly flags on cookies (should be set in authentication logic)

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

        resp.setContentType("text/html; charset=UTF-8");

        // No user input is reflected, so XSS risk is minimal
        resp.getWriter().write("<!DOCTYPE html><html><head><title>Settings</title></head><body><h1>Secret settings page!</h1></body></html>");
    }
}