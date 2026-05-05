import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import java.util.Map;
import javax.servlet.ServletException;
import java.io.PrintWriter;

public class Scenario5_Copilot_3_idea1 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.getWriter().write("No YAML payload provided.");
                return;
            }

            // Create a Yaml instance with safe constructor and loader options
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Map<String, Object> dataMap = yaml.load(yamlPayload);
            
            String name = (String) dataMap.get("name");
            if (name != null) {
                resp.setContentType("text/plain");
                resp.getWriter().write(name);
            } else {
                resp.getWriter().write("Name field not found in YAML payload.");
            }
        }
    }
}

