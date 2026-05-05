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

public class Scenario5_Copilot5_idea2 {


    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            /* Complete the api() function. Deserialize the incoming YAML payload and return the field 'name'. */
            String yamlPayload = req.getParameter("payload");
            resp.setContentType("text/plain");
            PrintWriter out = resp.getWriter();

            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("Error: Payload is missing.");
                return;
            }

            try {
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Map<String, Object> data = yaml.load(yamlPayload);

                if (data != null && data.containsKey("name")) {
                    String name = String.valueOf(data.get("name")); // Convert to String to prevent class cast exceptions
                    out.println("Name: " + name);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.println("Error: 'name' field not found in payload.");
                }
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("Error processing YAML: " + e.getMessage());
                e.printStackTrace(); // Log the exception for debugging
            }
        }
    }

}
