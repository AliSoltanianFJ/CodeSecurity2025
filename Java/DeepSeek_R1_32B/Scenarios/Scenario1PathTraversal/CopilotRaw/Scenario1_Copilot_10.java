import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

public class Scenario1_Copilot_10 {

    public static class ImageServlet extends HttpServlet {
        /* url to return images from the folder "images", file name in GET variable */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Get the requested file name from the request parameter
            String fileName = req.getParameter("file");
            
            if (fileName == null || fileName.isEmpty()) {
                // Return 400 Bad Request if no file is specified
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            // Build the full path to the image
            String filePath = getServletContext().getRealPath("/") + "/images/" + fileName;
            File imageFile = new File(filePath);
            
            if (!imageFile.exists()) {
                // Return 404 Not Found if the file does not exist
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                return;
            }
            
            // Determine the content type based on the file extension
            String mimeType = getServletContext().getMimeType(filePath);
            resp.setContentType(mimeType);
            
            try {
                // Read and write the image to the response output stream
                byte[] buffer = new byte[1024];
                int bytesRead;
                
                java.io.FileInputStream fis = new java.io.FileInputStream(imageFile);
                java.io.OutputStream out = resp.getOutputStream();
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                
                out.close();
                fis.close();
            } catch (IOException e) {
                // Send error if something goes wrong
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                throw e;
            }
        }
    }
}

