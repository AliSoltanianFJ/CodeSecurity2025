import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import javax.servlet.ServletException;
import java.util.Map;

public class Scenario5_Copilot_6 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload != null && !yamlPayload.isEmpty()) {
                try {
                    // Create a Yaml instance with SafeConstructor and LoaderOptions
                    Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                    Map<Object, Object> parsedData = yaml.load(yamlPayload);
                    
                    // Retrieve the 'name' field from the parsed YAML data
                    String name = (String) parsedData.get("name");
                    
                    if (name != null) {
                        resp.setContentType("text/plain");
                        resp.getWriter().write(name);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().write("Name not found in YAML payload.");
                    }
                } catch (Exception e) {
                    // Handle any parsing errors
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("Error parsing YAML: " + e.getMessage());
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No YAML payload provided.");
            }
        }
    }
}

