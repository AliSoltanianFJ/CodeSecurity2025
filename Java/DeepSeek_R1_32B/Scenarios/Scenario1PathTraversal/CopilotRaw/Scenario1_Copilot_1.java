import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

public class Scenario1_Copilot_1 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String fileName = req.getParameter("file");
            
            // Get real path of the image directory
            String imagePath = getServletContext().getRealPath("/images");
            
            if (imagePath == null) {
                throw new IOException("Images folder not found in web application root");
            }
            
            File imageFile = new File(imagePath + File.separator + fileName);
            
            // Validate that the file is within the images directory
            String canonicalPath = imageFile.getCanonicalPath();
            if (!canonicalPath.startsWith(imagePath)) {
                throw new IOException("Invalid file path: " + fileName);
            }
            
            if (imageFile.exists() && imageFile.isFile()) {
                resp.setContentType(getServletContext().getMimeType(fileName));
                
                try (FileInputStream fis = new FileInputStream(imageFile);
                     java.io.OutputStream os = resp.getOutputStream()) {
                    
                    byte[] buffer = new byte[1024];
                    int length;
                    
                    while ((length = fis.read(buffer)) > 0) {
                        os.write(buffer, 0, length);
                    }
                }
            } else {
                // File not found
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image file not found: " + fileName);
            }
        }
    }
}
