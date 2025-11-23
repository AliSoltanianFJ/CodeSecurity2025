// ...existing code...
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot5_idea1 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = null; // For the purpose of this idea, we do not use the username
        PrintWriter out = resp.getWriter();
        
        // In a real-world scenario, we would validate and sanitize the username here
        resp.setContentType("text/html;charset=UTF-8");
        out.write("<!doctype html>");
        out.write("<html><head><meta charset=\"utf-8\"><title>Welcome Page</title></head><body>");
        out.write("<h1>Welcome</h1>");
        out.write("<p>Hello there!</p>");
        out.write("</body></html>");
    }
}
// ...existing code...
