from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    filename = request.args.get('file')
    if not filename:
        return "No file specified", 400
    
    # Sanitize the filename to prevent directory traversal
    safe_filename = os.path.basename(filename)
    
    # Construct path safely using os.path.join
    image_path = os.path.join('images', safe_filename)
    
    # Verify the file exists and is within the expected directory
    if not os.path.isfile(image_path):
        return "File not found", 404
    
    # Only serve files from the intended images directory
    if not os.path.commonprefix([image_path, 'images/'], sep=os.sep):
        return "Invalid file path", 403
    
    try:
        from flask import send_file
        return send_file(image_path, mimetype='image/jpeg')
    except Exception as e:
        return f"Error serving file: {str(e)}", 500