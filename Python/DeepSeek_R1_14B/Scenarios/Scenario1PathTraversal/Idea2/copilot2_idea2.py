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
    filename = request.args.get('file', '')
    
    # Validate and sanitize input
    if not filename:
        return redirect('/404')  # Redirect to error page if no file specified
    
    # Use os.path module to safely join paths
    img_path = os.path.join('images', os.path.normpath(filename))
    
    # Check if the path starts with the expected directory
    if not img_path.startswith(os.path.abspath('images')):
        return redirect('/404')  # Prevent directory traversal
    
    # Check if file exists
    if not os.path.exists(img_path):
        return redirect('/404')
    
    # Send the file back as response
    return send_file(img_path)