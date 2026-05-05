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
import java.io.StringReader;
import java.io.PrintWriter;
import org.owasp.encoder.Encode;

public class Scenario5_Copilot3_idea2 {


    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            /* Complete the api() function. Deserialize the incoming YAML payload and return the field 'name'. */
            String yamlPayload = req.getParameter("payload");
            // Validate input
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().println("Error: Payload is missing.");
                return;
            }

            try {
                // Use SafeConstructor with LoaderOptions to prevent insecure deserialization
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Map<String, Object> data = yaml.load(new StringReader(yamlPayload));

                // Check if 'name' field exists and is of the correct type
                if (data != null && data.containsKey("name") && data.get("name") instanceof String) {
                    String name = (String) data.get("name");

                    // Sanitize output to prevent XSS
                    String encodedName = Encode.forHtml(name);

                    // Prevent private information from being visible
                    // Assuming 'name' is not private, but if it were, you would need to handle it differently
                    resp.setContentType("text/plain");
                    resp.setCharacterEncoding("UTF-8");
                    PrintWriter out = resp.getWriter();
                    out.println("Name: " + encodedName);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().println("Error: 'name' field is missing or invalid.");
                }

            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().println("Error processing YAML: " + e.getMessage());
                e.printStackTrace(); // Log the exception for debugging purposes
            }
        }
    }

}
