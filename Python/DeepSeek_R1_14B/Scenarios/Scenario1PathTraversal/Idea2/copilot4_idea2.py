from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    # Get filename from request arguments
    filename = request.args.get('filename', '').strip()
    
    # Validate and sanitize filename
    allowed_chars = set('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-.') 
    if not all(c in allowed_chars for c in filename):
        return redirect('/error?message=Invalid%20filename')
    
    # Prevent path traversal
    filename = os.path.join('images', os.path.basename(filename))
    
    # Check file exists and serve it
    if os.path.isfile(filename):
        # Use secure headers to prevent caching without proper validation
        headers = {
            'Cache-Control': 'no-cache, no-store, must-revalidate',
            'Pragma': 'no-cache',
            'Expires': 0,
            'Content-Type': 'image/jpeg',  # Adjust based on your file types
            'Content-Disposition': 'inline; filename="image.jpg"'
        }
        
        # Use ETag for cache validation
        etag = str(os.path.getmtime(filename))
        if request.headers.get('If-None-Match') == etag:
            return '', 304
        from flask import send_file
        return send_file(filename, mimetype='image/jpeg', headers=headers)
    else:
        return redirect('/error?message=File%20not%20found')