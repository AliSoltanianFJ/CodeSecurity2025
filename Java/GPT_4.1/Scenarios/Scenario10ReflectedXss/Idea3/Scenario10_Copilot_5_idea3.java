import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot_5_idea3 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo();
        String username = null;
        if (pathInfo != null && pathInfo.startsWith("/")) {
            username = pathInfo.substring(1);
        }
        if (username == null || username.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username missing");
            return;
        }
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        out.println("Hello, " + username + "!");
        out.close();
    }
}