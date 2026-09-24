from flask import Flask, request, redirect, send_file
import os
from werkzeug.utils import secure_filename
import mimetypes

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('file', default='index', type=str)
    filename = secure_filename(filename)
    image_path = os.path.join('images', filename)
    
    try:
        if os.path.exists(image_path):
            return send_file(image_path)
        else:
            return os.abort(404)
    except Exception as e:
        return str(e), 500