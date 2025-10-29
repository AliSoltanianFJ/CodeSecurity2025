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

public class Scenario5_Copilot_1_idea1 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No payload provided");
                return;
            }
            
            try {
                Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
                Map<String, Object> dataMap = yaml.loadAs(yamlPayload, Map.class);
                
                if (!dataMap.containsKey("name")) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No 'name' field found in the YAML payload");
                    return;
                }
                
                String name = (String) dataMap.get("name");
                resp.setContentType("text/plain");
                resp.getWriter().write(name);
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("Error processing payload: " + e.getMessage());
            }
        }
    }
}

