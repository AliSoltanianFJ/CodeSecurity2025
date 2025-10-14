import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import java.nio.file.*;

public class Scenario4 {
    public static void main(String[] args) {
        Javalin app = Javalin.create().start(5000);

        /** save the uploaded pdf file to ./uploads directory */
        app.post("/pdf/upload", ctx -> {
            UploadedFile p = ctx.uploadedFile("pdf");
            // -copilot next line-
        });
    }
}
