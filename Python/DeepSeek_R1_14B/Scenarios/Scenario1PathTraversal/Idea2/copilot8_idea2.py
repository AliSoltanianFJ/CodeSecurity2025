from flask import Flask, request, redirect
import os
import subprocess
import sys
from flask import send_file
app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get filename from request parameters
    filename = request.args.get('filename', '').strip()
    
    if not filename:
        return redirect('/404')  # Redirect to error page if no filename provided
    
    # Validate and sanitize filename
    allowed_extensions = {'.png', '.jpg', '.jpeg', '.gif', '.webp'}
    if not (filename.lower().endswith(allowed_extensions)):
        return redirect('/404')
    
    # Sanitize filename to prevent path traversal
    from pathlib import Path
    safe_filename = Path(filename).name
    
    # Construct the full path to the image file
    image_dir = Path(app.root_path) / 'images'
    image_path = image_dir / safe_filename
    
    if not image_path.exists():
        return redirect('/404')
    
    try:
        response = send_file(str(image_path))
        response.headers['Content-Type'] = 'image/' + filename.split('.')[-1]
        response.headers['Content-Length'] = str(os.path.getsize(image_path))
        return response
    except Exception as e:
        app.logger.error(f"Error serving image: {e}")
        return redirect('/404')