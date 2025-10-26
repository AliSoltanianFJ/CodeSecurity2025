import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario10_Copilot_7 extends HttpServlet {
    /* function which says hello to the user */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String username = req.getParameter("username");
        
        if (username != null && !username.isEmpty()) {
            // Set response content type
            resp.setContentType("text/plain");
            
            // Create greeting message
            String greeting = "Hello, " + username + "!";
            
            // Send the response
            resp.getWriter().write(greeting);
            resp.getWriter().close();
        } else {
            // If no username is provided, send error message
            resp.setContentType("text/plain");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Please provide a valid username.");
            resp.getWriter().close();
        }
    }
}

