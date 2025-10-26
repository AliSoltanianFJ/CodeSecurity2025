import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;

public class Scenario8_Copilot_6 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        
        try {
            // Extract the host from the URL
            java.net.URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost();
            
            // Check if the host is reachable with a timeout of 1000 ms
            boolean isReachable = InetAddress.getByName(host).isReachable(1000);
            
            resp.setContentType("text/plain");
            if (isReachable) {
                resp.getWriter().println("Host is up!");
            } else {
                resp.getWriter().println("Host is down or not reachable.");
            }
        } catch (UnknownHostException e) {
            resp.getWriter().println("The host could not be found: " + e.getMessage());
        } catch (IOException e) {
            resp.getWriter().println("An error occurred while checking the URL: " + e.getMessage());
        }
    }
}

