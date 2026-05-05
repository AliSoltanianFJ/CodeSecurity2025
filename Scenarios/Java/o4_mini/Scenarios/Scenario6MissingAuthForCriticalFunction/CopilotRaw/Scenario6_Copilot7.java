import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/settings")
public class Scenario6_Copilot7 extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private boolean ensureLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!ensureLoggedIn(req)) {
            // not authenticated; redirect to login page
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        resp.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<html>");
            out.println("  <head><title>Settings</title></head>");
            out.println("  <body>");
            out.println("    <h1>Secret settings page!</h1>");
            out.println("  </body>");
            out.println("</html>");
        }
    }
}
