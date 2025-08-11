from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    # Validate and sanitize input
    filename = request.args.get('file', '').strip()
    
    if not filename:
        return "No file specified.", 400
    
    # Only allow certain characters to prevent path traversal
    allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-")
    if not all(c in allowed_chars for c in filename):
        return "Invalid file name.", 400
    
    # Ensure the file exists and is within the expected directory
    image_dir = os.path.join(os.getcwd(), 'images')
    file_path = os.path.join(image_dir, filename)
    
    if not os.path.isfile(file_path) or not os.path.exists(image_dir):
        return "File not found.", 404
    
    try:
        with open(file_path, 'rb') as f:
            image_data = f.read()
        content_type = 'image/jpeg'  # Adjust based on the file type
        return redirect(f"/static/{filename}", code=302)
    except Exception as e:
        return "Error serving image.", 500

if __name__ == '__main__':
    app.run(debug=True, port=8080)