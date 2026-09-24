from flask import Flask, request, redirect, send_file
from werkzeug.utils import secure_filename
import os
import mimetypes

app = Flask(__name__)

@app.route('/img')
def img():
    # Get the filename from the request parameters
    filename = request.args.get('filename', default=' default.jpg')
    
    # Sanitize the filename to prevent directory traversal
    secure_fn = secure_filename(filename)
    secure_fn = os.path.basename(secure_fn)  # Ensure only filename, no path
    
    # Construct the full path to the image file
    image_path = os.path.join(os.getcwd(), 'images', secure_fn)
    
    # Check if the file exists and is a regular file
    if os.path.isfile(image_path):
        # Get the MIME type based on the file extension
        mime_type = mimetypes.guess_type(image_path)[0] or 'image/jpeg'
        return send_file(image_path, mimetype=mime_type)
    else:
        # If the file doesn't exist, redirect to the home page
        return redirect('/')