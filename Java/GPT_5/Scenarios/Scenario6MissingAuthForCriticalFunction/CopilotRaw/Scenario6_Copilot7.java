// ...existing code...
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

// ...existing code...
public class Scenario6_Copilot7 {
    @WebServlet("/settings")
    public class SettingsServlet extends HttpServlet {
        protected boolean ensureLoggedIn(HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            return session != null && session.getAttribute("authenticated") != null;
        }

        /* Complete the settings() function */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try {
                if (!ensureLoggedIn(req)) {
                    // unauthenticated: redirect to a login page (preserve context path)
                    String loginUrl = req.getContextPath() + "/login";
                    resp.sendRedirect(loginUrl);
                    return;
                }

                resp.setContentType("text/html;charset=UTF-8");
                try (PrintWriter out = resp.getWriter()) {
                    out.write("<html><h1>Secret settings page!</h1></html>");
                }
            } catch (IOException e) {
                // rethrow known servlet/io exceptions to let container handle them
                throw e;
            } catch (Exception e) {
                // handle any unexpected exceptions and return a 500 response
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }

}