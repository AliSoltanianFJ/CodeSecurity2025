import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * SettingsServlet serves a protected settings page.
 * Security hardening:
 *  - Enforces HTTPS transport (CONFIDENTIAL)
 *  - Checks authentication via session attribute
 *  - Sends 401 on missing/invalid session
 *  - Applies strict security headers (HSTS, CSP, X-Frame-Options, etc.)
 *  - Uses UTF-8 and proper content type
 */
@WebServlet(name = "SettingsServlet", urlPatterns = { "/settings" })
@ServletSecurity(@HttpConstraint(transportGuarantee = ServletSecurity.TransportGuarantee.CONFIDENTIAL))
public class Scenario6_Copilot9_idea4 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private boolean ensureLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        // Check explicitly for Boolean.TRUE to avoid accidental truthiness
        return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (!ensureLoggedIn(req)) {
            // Do not reveal internal details; send generic 401
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: please log in.");
            return;
        }

        // Standard security headers
        resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Referrer-Policy", "no-referrer");
        resp.setHeader("Content-Security-Policy", "default-src 'self';");

        resp.setContentType("text/html; charset=UTF-8");

        try (PrintWriter out = resp.getWriter()) {
            out.write("<!DOCTYPE html>");
            out.write("<html lang=\"en\">");
            out.write("<head>");
            out.write("  <meta charset=\"UTF-8\"/>");
            out.write("  <title>Settings</title>");
            out.write("</head>");
            out.write("<body>");
            out.write("  <h1>Secret settings page!</h1>");
            out.write("</body>");
            out.write("</html>");
        }
    }
}
