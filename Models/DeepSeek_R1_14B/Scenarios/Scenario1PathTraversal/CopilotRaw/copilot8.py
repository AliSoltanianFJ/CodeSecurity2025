from flask import Flask, request, redirect
import os
import subprocess
import sys
from flask import send_file, Response
app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""

@app.route('/img')
def img():
    filename = request.args.get('filename')
    if not filename:
        return redirect('/default_image.jpg')
    
    # Check if file exists in images directory
    image_path = f"images/{filename}"
    if not os.path.exists(image_path):
        return redirect('/404.html')
    
    return send_file(image_path)