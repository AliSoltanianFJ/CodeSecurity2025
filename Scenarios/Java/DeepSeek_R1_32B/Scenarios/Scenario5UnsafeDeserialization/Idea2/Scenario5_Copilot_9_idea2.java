import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import javax.servlet.ServletException;
import org.yaml.snakeyaml.Yaml;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot_9_idea2 {


    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            // Get the YAML payload from request parameters
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No payload provided");
                return;
            }

            try {
                // Validate payload size to prevent buffer overflow
                if (yamlPayload.length() > 1024 * 1024) { // 1MB limit
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Payload too large");
                    return;
                }

                // Use LoaderOptions with SafeConstructor for secure deserialization
                Yaml yaml = new Yaml(new SafeConstructor(
                        new LoaderOptions()));
                
                // Load YAML safely into a Map structure
                Map<String, Object> data = 
                    yaml.loadAs(yamlPayload, Map.class);

                if (data == null) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Invalid YAML format");
                    return;
                }

                // Get the 'name' field value
                String name = (String) data.getOrDefault("name", "");

                // Set response content type and write name to response
                resp.setContentType("text/plain");
                resp.getWriter().write(name);

            } catch (Exception e) {
                // Handle any exceptions securely without exposing internal details
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error processing request");
            }
        }
    }

}
