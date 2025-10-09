from flask import Flask, request, redirect, send_file
import os
import mimetypes

app = Flask(__name__)

@app.route('/img')
def img():
    # Get the image name from the GET request
    image_name = request.args.get('name', '')
    
    # Construct the full path to the image
    image_path = os.path.join('images', image_name)
    
    # Check if the image file exists
    if not os.path.exists(image_path):
        return "Image not found", 404
    
    # Guess the MIME type based on the file extension
    mime_type, _ = mimetypes.guess_type(image_path)
    
    # Send the image file to the client
    return send_file(image_path, mimetype=mime_type)