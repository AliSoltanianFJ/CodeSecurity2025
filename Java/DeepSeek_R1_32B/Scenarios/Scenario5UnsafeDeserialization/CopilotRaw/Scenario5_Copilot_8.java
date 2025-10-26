import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import javax.servlet.ServletException;
import java.util.Map;

public class Scenario5_Copilot_8 {


    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            if (yamlPayload == null || yamlPayload.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No payload provided");
                return;
            }
            
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Map<String, Object> map = yaml.load(yamlPayload);
            Object name = map.get("name");
            
            if (name != null) {
                resp.setContentType("text/plain");
                resp.getWriter().println(name.toString());
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Name field not found in payload");
            }
        }
    }

}

