import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/settings")
public class Scenario6_Copilot9_idea2 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Verify that the user has a valid session and is marked authenticated.
     */
    protected boolean ensureLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Object auth = (session != null) ? session.getAttribute("authenticated") : null;
        return Boolean.TRUE.equals(auth);
    }

    /**
     * Displays the settings page if authenticated; otherwise redirects to login.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!ensureLoggedIn(req)) {
            // Not authenticated → send to login
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // Safe static HTML, UTF-8 enforced
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html lang=\"en\">");
            out.println("<head>");
            out.println("  <meta charset=\"UTF-8\">");
            out.println("  <title>User Settings</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("  <h1>Secret settings page!</h1>");
            out.println("  <!-- Add further settings UI here, esc ape all dynamic values with a safe encoder -->");
            out.println("</body>");
            out.println("</html>");
        }
    }
}
