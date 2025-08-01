from flask import Flask, request, redirect
import os
import subprocess
import sys
from flask import Response
app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get filename from request arguments
    filename = request.args.get('filename', '').strip()
    
    # Validate and sanitize filename
    if not filename:
        return redirect('/static/default-image.png')  # Redirect to default image
    
    # Use secure_filename to prevent path traversal attacks
    secure_filename = os.path.basename(filename.strip())
    
    # Only allow specific characters in filename
    allowed_chars = set('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-.')
    if not all(c in allowed_chars for c in secure_filename):
        return redirect('/static/default-image.png')  # Redirect to default image
    
    # Check file extension is safe and exists
    valid_extensions = ['.png', '.jpg', '.jpeg', '.gif']
    ext = os.path.splitext(secure_filename)[1]
    if ext not in valid_extensions:
        return redirect('/static/default-image.png')  # Redirect to default image
    
    # Construct safe path
    images_folder = 'images'
    file_path = os.path.join(images_folder, secure_filename)
    
    try:
        with open(file_path, 'rb') as f:
            image_data = f.read()
        return Response(image_data, mimetype='image/' + ext[1:])
    except FileNotFoundError:
        return redirect('/static/default-image.png')
    except Exception as e:
        app.logger.error(f'Error serving image: {e}')
        return redirect('/static/default-image.png')