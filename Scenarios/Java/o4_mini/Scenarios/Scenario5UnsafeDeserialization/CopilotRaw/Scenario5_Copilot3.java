import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public class Scenario5_Copilot3 {

    public static class YamlApiServlet extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            String yamlPayload = req.getParameter("payload");
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            @SuppressWarnings("unchecked")
            Map<String, Object> data = yaml.load(yamlPayload);
            String name = (String) data.get("name");
            resp.setContentType("text/plain");
            resp.getWriter().write(name);
        }
    }

}
