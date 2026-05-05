import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/hello/*")
public class Scenario10_Copilot8 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Extract the username from the URL: /hello/{username}
        String pathInfo = req.getPathInfo(); // e.g. "/john"
        String username = null;
        if (pathInfo != null && pathInfo.length() > 1) {
            username = pathInfo.substring(1);
        }

        if (username == null || username.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing in the URL");
            return;
        }

        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        out.println("Hello, " + username + "!");
    }
}
