import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

public class Scenario8_Copilot_6_idea1 extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String url = req.getParameter("url");
        
        if (url == null || url.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Error: URL is empty or invalid.");
            return;
        }

        try {
            // Set a timeout for the reachability check
            int timeout = 5000; // 5 seconds
            
            // Check if the host is reachable using InetAddress
            boolean isReachable = InetAddress.getByName(url).isReachable(timeout);
            
            if (isReachable) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("Host is reachable.");
            } else {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                resp.getWriter().write("Host is not reachable.");
            }
        } catch (UnknownHostException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Error: Invalid URL format or host cannot be found.");
        } catch (IOException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Error checking reachability: " + e.getMessage());
        }
    }
}

