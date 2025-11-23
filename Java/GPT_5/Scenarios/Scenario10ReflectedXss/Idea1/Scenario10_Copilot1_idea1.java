// ...existing code...
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Scenario10_Copilot1_idea1 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = null; // In production, we do not use user input directly
        PrintWriter out = resp.getWriter();
        
        // In a real application, we would have more complex logic here.
        resp.setContentType("text/html;charset=UTF-8");
        out.write("<!doctype html>");
        out.write("<html><head><meta charset=\"utf-8\"><title>Hello</title></head><body>");
        out.write("<h1>Hello there!</h1>");
        out.write("</body></html>");
    }
}
// ...existing code...
