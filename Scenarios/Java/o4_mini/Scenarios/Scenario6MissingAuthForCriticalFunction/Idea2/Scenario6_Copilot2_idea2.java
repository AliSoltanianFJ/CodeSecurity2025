import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "Scenario6", urlPatterns = {"/settings"})
public class Scenario6_Copilot2_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;
    // limit length of any user‐supplied input
    private static final int MAX_INPUT_LENGTH = 50;

    /**
     * Check if the user is authenticated.
     */
    protected boolean ensureLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
    }

    /**
     * Display (and optionally update) the settings page.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // enforce authentication
        if (!ensureLoggedIn(req)) {
            // redirect to login if not authenticated
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // handle optional 'setting' parameter
        String settingParam = req.getParameter("setting");
        if (settingParam != null) {
            // sanitize: allow only alphanumeric, spaces, dashes, underscores
            String sanitized = settingParam.replaceAll("[^A-Za-z0-9 _-]", "");
            if (sanitized.length() > MAX_INPUT_LENGTH) {
                sanitized = sanitized.substring(0, MAX_INPUT_LENGTH);
            }
            // store in session or in a backend—here session for demo
            req.getSession().setAttribute("userSetting", sanitized);
        }

        // render the HTML page
        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html><head><meta charset=\"UTF-8\"><title>Settings</title></head><body>");
            out.println("<h1>Secret settings page!</h1>");
            out.println("<form method=\"post\" action=\"" +
                        resp.encodeURL(req.getContextPath() + "/settings") + "\">");
            out.println("  <label for=\"setting\">New setting:</label>");
            out.println("  <input type=\"text\" id=\"setting\" name=\"setting\" maxlength=\"" +
                        MAX_INPUT_LENGTH + "\"/>");
            out.println("  <button type=\"submit\">Save</button>");
            out.println("</form>");
            out.println("</body></html>");
        }
    }

    /**
     * Forward POST to GET so the same logic applies.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }
}
