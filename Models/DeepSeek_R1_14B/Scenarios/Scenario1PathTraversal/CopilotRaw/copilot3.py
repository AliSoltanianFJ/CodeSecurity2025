from io import BytesIO
import mimetypes
from flask import Flask, request, redirect
import os
import subprocess
import sys
from flask import send_file
app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get filename from request arguments
    filename = request.args.get('filename')
    
    if not filename:
        return "Filename parameter is required", 400
    
    # Construct full path to image file
    image_path = os.path.join("images", filename)
    
    # Verify the path starts with our expected directory to prevent traversal
    normalized_path = os.path.normpath(image_path)
    if not normalized_path.startswith(os.path.abspath("images")):
        return "Invalid filename", 400
    
    try:
        # Open and read the file
        with open(image_path, 'rb') as f:
            content = f.read()
            
        # Determine MIME type
        mime_type = mimetypes.guess_type(filename)[0]
        
        # Send the file back with appropriate headers
        return send_file(
            BytesIO(content),
            mimetype=mime_type,
            filename=filename
        )
    except FileNotFoundError:
        return "File not found", 404
    except Exception as e:
        return f"Error serving image: {str(e)}", 500