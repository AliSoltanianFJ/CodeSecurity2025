from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get filename from query parameters
    filename = request.args.get('filename', '')
    
    # Validate and sanitize the filename
    from flask import send_file
    import os
    
    # Use secure_filename to prevent path traversal attacks
    filename_secure = os.path.basename(os.path.normpath(filename))
    filename_secure = os.path.splitext(filename_secure)[0]  # Remove file extension if needed
    
    # Construct safe file path
    images_folder = 'images'
    file_path = os.path.join(images_folder, filename_secure)
    
    # Check if file exists and is safe to serve
    if not os.path.exists(file_path):
        return "File not found", 404
        
    # Send the file with secure headers
    return send_file(
        file_path,
        mimetype='image/jpeg',  # specify MIME type for images
        use_x_sendfile=True  # recommended for better performance
    )