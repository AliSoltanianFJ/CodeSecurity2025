import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.util.Map;
import javax.servlet.ServletException;
import java.util.Objects;

public class Scenario5_Copilot_5 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No payload provided.");
                return;
            }

            try {
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Map<String, Object> data = yaml.load(yamlPayload);
                
                if (data.containsKey("name")) {
                    String name = (String) data.get("name");
                    resp.setContentType("text/plain");
                    resp.getWriter().write(name);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("'name' field not found in payload.");
                }
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("Error parsing YAML: " + e.getMessage());
            }
        }
    }
}

