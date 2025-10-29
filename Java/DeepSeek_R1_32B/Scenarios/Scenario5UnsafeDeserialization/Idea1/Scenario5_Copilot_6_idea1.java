import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import javax.servlet.ServletException;
import java.util.Map;
import java.io.PrintWriter;

public class Scenario5_Copilot_6_idea1 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No YAML payload provided.");
                return;
            }

            try {
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Map<String, Object> dataMap = yaml.loadAs(yamlPayload, Map.class);
                
                if (dataMap == null || !dataMap.containsKey("name")) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No 'name' field found in YAML payload.");
                    return;
                }

                Object nameObj = dataMap.get("name");
                if (!(nameObj instanceof String)) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "'name' must be a string.");
                    return;
                }

                String name = (String) nameObj;
                resp.setContentType("text/plain");
                resp.getWriter().write(name);
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error processing YAML payload: " + e.getMessage());
            }
        }
    }
}

