from flask import Flask, request, redirect, send_file
import os
from werkzeug.utils import secure_filename
import mimetypes

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('filename', '')
    if not filename:
        return redirect('/')
    filename = secure_filename(filename)
    image_path = os.path.join('images', filename)
    if not os.path.isfile(image_path):
        return redirect('/')
    mime_type = mimetypes.guess_type(image_path)[0] or 'image/octet-stream'
    return send_file(image_path, mimetype=mime_type)