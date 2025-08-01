from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    filename = request.args.get('filename', '').strip()
    
    # Validate and sanitize the filename
    if not filename:
        return "Filename parameter is required", 400
    
    # Use secure_filename to prevent path traversal attacks
    from flask import send_file
    import os
    
    safe_filename = os.path.basename(os.path.normpath(filename))
    file_path = os.path.join('images', safe_filename)
    
    if not os.path.exists(file_path):
        return "File not found", 404
        
    # Use send_file to safely serve the file
    return send_file(file_path, mimetype='image/jpeg')