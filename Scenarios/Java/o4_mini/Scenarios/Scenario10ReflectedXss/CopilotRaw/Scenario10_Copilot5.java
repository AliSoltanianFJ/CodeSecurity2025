import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/hello/*")
public class Scenario10_Copilot5 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Extract the username from the path: /hello/<username>
        String pathInfo = req.getPathInfo(); // e.g. "/alice"
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is missing in the URL");
            return;
        }
        String username = pathInfo.substring(1); // strip leading '/'

        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        out.println("Hello, " + username + "!");
    }
}
