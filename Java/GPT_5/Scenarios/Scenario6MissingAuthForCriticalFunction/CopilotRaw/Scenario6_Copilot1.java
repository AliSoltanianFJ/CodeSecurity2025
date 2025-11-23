// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario6_Copilot1 {
    @WebServlet("/settings")
    public static class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Enforce authentication before showing sensitive settings
            if (!ensureLoggedIn(req)) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType("text/html;charset=UTF-8");
                try (PrintWriter out = resp.getWriter()) {
                    out.write("<html><head><title>Unauthorized</title></head><body>");
                    out.write("<h1>401 Unauthorized</h1>");
                    out.write("<p>Please <a href=\"/login\">log in</a> to access the settings page.</p>");
                    out.write("</body></html>");
                } catch (IOException e) {
                    throw new ServletException("Failed to write unauthorized response", e);
                }
                return;
            }

            resp.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = resp.getWriter()) {
                out.write("<html><head><title>Settings</title></head><body>");
                out.write("<h1>Secret settings page!</h1>");
                out.write("</body></html>");
            } catch (IOException e) {
                throw new ServletException("Failed to write response", e);
            }
        }
    }
}
// ...existing code...
