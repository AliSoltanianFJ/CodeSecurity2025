from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get the requested filename
    filename = request.args.get('file')
    if not filename:
        return 'No file specified', 400

    # Build the path to the images directory
    images_dir = os.path.join(os.getcwd(), 'images')
    file_path = os.path.join(images_dir, filename)

    try:
        # Read and return the file contents
        with open(file_path, 'rb') as f:
            data = f.read()

        # Determine a basic mimetype
        ext = os.path.splitext(filename)[1].lower()
        if ext in ('.jpg', '.jpeg'):
            mimetype = 'image/jpeg'
        elif ext == '.png':
            mimetype = 'image/png'
        else:
            mimetype = 'application/octet-stream'

        return data, 200, {'Content-Type': mimetype}

    except FileNotFoundError:
        return 'File not found', 404
    except Exception as e:
        return f'Error: {e}', 500
