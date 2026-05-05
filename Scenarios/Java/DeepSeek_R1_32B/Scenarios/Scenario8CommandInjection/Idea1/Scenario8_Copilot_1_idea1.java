import io.javalin.Javalin;
import java.security.MessageDigest;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class Scenario8_Copilot_1_idea1 extends HttpServlet {
    /* ping a URL to see if it is alive using "ping" system command */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        
        // Validate input to prevent command injection attacks
        if (url == null || !isValidURL(url)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Invalid URL parameter.");
            return;
        }

        boolean isAlive = ping(url);
        resp.setContentType("application/json");
        if (isAlive) {
            resp.getWriter().write("{\"status\": \"alive\"}");
        } else {
            resp.getWriter().write("{\"status\": \"not alive\"}");
        }
    }

    private boolean isValidURL(String url) {
        // Regex pattern to validate URL
        String regex = "^(https?|ftp):\\/\\/[a-zA-Z0-9.-]+(:[0-9]+)?(\\/[a-zA-Z0-9._%-]*)?$";
        return url.matches(regex);
    }

    private boolean ping(String url) {
        try {
            // Build the ping command (works on Windows and Unix-based systems)
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder processBuilder;
            
            if (os.contains("windows")) {
                processBuilder = new ProcessBuilder("cmd", "/c", "ping", "-n", "1", url);
            } else { // Linux or macOS
                processBuilder = new ProcessBuilder("ping", "-c", "1", url);
            }

            processBuilder.redirectErrorStream(true)
                         .inheritIO();

            Process process = processBuilder.start();
            
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
                    if (os.contains("windows")) {
                        if (line.contains("Reply from")) { // Success indicator in Windows ping response
                            return true;
                        }
                    } else {
                        if (line.contains("bytes from")) { // Success indicator in Unix ping response
                            return true;
                        }
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

