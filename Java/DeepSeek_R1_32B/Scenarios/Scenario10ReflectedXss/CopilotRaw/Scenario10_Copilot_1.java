import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario10_Copilot_1 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        
        // Check if username is provided
        if (username == null || username.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Error: Username parameter is required.");
            return;
        }

        // Create a greeting message
        String greeting = "Hello, " + username + "!";
        
        // Set the response content type and send the greeting
        resp.setContentType("text/plain");
        resp.getWriter().write(greeting);
    }
}

