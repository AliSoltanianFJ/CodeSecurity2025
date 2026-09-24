from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

from flask import send_file
import werkzeug

@app.route('/img')
def img():
    # Get the file name parameter from the request
    file_name = request.args.get('file', '')
    
    # Redirect to home page if file name is not provided
    if not file_name:
        return redirect('/')
    
    # Sanitize the file name to prevent path traversal attacks
    secure_name = werkzeug.secure_filename(file_name)
    
    # Construct the full file path
    image_path = os.path.join('images', secure_name)
    
    # Check if the file exists and is a regular file
    if os.path.exists(image_path) and os.path.isfile(image_path):
        # Define allowed image extensions
        allowed_extensions = {'png', 'jpg', 'jpeg', 'gif', 'bmp'}
        file_extension = os.path.splitext(secure_name)[1].lower()
        
        if file_extension in allowed_extensions:
            try:
                # Open and send the image file
                with open(image_path, 'rb') as f:
                    return send_file(f)
            except Exception as e:
                return str(e), 500

    # Redirect to home page if the file is not found or invalid
    return redirect('/')