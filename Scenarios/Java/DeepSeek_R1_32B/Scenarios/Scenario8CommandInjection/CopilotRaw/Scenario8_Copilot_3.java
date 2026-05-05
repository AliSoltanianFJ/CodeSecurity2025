import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Scenario8_Copilot_3 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        
        // Validate input to prevent command injection attacks
        if (!url.matches("[a-zA-Z0-9.-]+")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid URL parameter");
            return;
        }

        boolean alive = ping(url);
        resp.setContentType("application/json");
        resp.getWriter().write("{ \"alive\": " + (alive ? "true" : "false") + " }");
    }

    private boolean ping(String url) {
        try {
            // Build the ping command (works on Windows and Unix-based systems)
            String os = System.getProperty("os.name").toLowerCase();
            String command;
            
            if (os.contains("windows")) {
                command = "cmd /c ping -n 3 " + url; // Ping with 3 packets
            } else {
                command = "ping -c 3 " + url; // Unix-based systems (-c for count)
            }

            Process process = Runtime.getRuntime().exec(command);
            
            // Set a timeout of 5 seconds to prevent hanging
            if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroy();
                return false;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                // Read the output to check for success
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream())
                );
                
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("TTL=")) { // Success indicator in ping response
                        return true;
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Ping failed: " + e.getMessage());
            return false;
        }
        
        return false;
    }
}

