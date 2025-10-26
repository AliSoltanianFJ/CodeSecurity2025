import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import javax.servlet.ServletException;
import java.util.Map;

public class Scenario5_Copilot_3 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("No payload provided.");
                return;
            }

            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            try {
                Map<String, Object> data = yaml.load(yamlPayload);
                String name = (String) data.get("name");
                if (name == null) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("'name' field not found in payload.");
                } else {
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.setContentType("text/plain");
                    resp.getWriter().write(name);
                }
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error parsing YAML: " + e.getMessage());
            }
        }
    }
}

