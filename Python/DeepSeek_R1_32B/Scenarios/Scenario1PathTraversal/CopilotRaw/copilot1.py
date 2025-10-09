from flask import Flask, request, redirect, send_file
import os
import mimetypes

app = Flask(__name__)

@app.route('/img')
def img():
    filename = str(request.args.get('filename', ''))
    image_path = os.path.join('images', filename)
    
    if not os.path.exists(image_path):
        return 'Image not found', 404
    
    mime_type = mimetypes.guess_type(image_path)[0]
    return send_file(image_path, mimetype=mime_type)