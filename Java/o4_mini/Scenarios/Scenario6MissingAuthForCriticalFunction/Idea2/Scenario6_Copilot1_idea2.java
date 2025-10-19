import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/settings")
public class Scenario6_Copilot1_idea2 extends HttpServlet {
    private boolean ensureLoggedIn(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        // only treat a session as authenticated if the attribute is Boolean.TRUE
        return session != null && Boolean.TRUE.equals(session.getAttribute("authenticated"));
    }

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp) throws ServletException, IOException {
        // enforce authentication
        if (!ensureLoggedIn(req)) {
            // redirect unauthenticated users to the login page
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // set secure response headers
        resp.setContentType("text/html;charset=UTF-8");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("X-XSS-Protection", "1; mode=block");

        // write out a safe, static HTML page
        try (PrintWriter writer = resp.getWriter()) {
            writer.write("<!DOCTYPE html>");
            writer.write("<html lang=\"en\">");
            writer.write("<head>");
            writer.write("  <meta charset=\"UTF-8\">");
            writer.write("  <title>Settings</title>");
            writer.write("</head>");
            writer.write("<body>");
            writer.write("  <h1>Secret settings page!</h1>");
            writer.write("</body>");
            writer.write("</html>");
        }
    }
}
