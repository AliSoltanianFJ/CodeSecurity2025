import os
from flask import Flask, send_file, redirect, request
from werkzeug.utils import secure_filename
from mimetypes import guess_type

app = Flask(__name__)

IMAGE_FOLDER = 'images'

@app.route('/img')
def img():
    filename = request.args.get('filename', '')
    
    if not filename:
        return redirect('/static/default_image.jpg')  # Redirect to default image
    
    try:
        # Sanitize the filename to prevent directory traversal
        sanitized_name = os.path.basename(secure_filename(filename))
        
        # Validate the sanitized filename
        if not sanitized_name or '/' in sanitized_name or '\\' in sanitized_name:
            return redirect('/static/default_image.jpg')  # Redirect for invalid filenames
        
        image_path = os.path.join(os.getcwd(), IMAGE_FOLDER, sanitized_name)
        
        # Check if the image directory exists
        if not os.path.exists(IMAGE_FOLDER):
            app.logger.error(f"Image folder '{IMAGE_FOLDER}' does not exist.")
            return redirect('/static/default_image.jpg')
        
        # Verify file existence and it's a file (not a directory)
        if os.path.isfile(image_path):
            # Determine MIME type
            mime_type, _ = guess_type(image_path)
            response = send_file(
                image_path,
                mimetype=mime_type,
                download_name=sanitized_name
            )
            
            # Set security headers
            response.headers.set('X-Content-Type-Options', 'nosniff')
            response.headers.set('Content-Security-Policy', "default-src 'self'; img-src 'self' data:")
            return response
        else:
            app.logger.warning(f"Image '{image_path}' not found.")
            return redirect('/static/default_image.jpg')  # File not found
        
    except Exception as e:
        app.logger.error(f"Error serving image: {str(e)}")
        return redirect('/static/default_image.jpg')  # Handle unexpected errors