from flask import Flask, request, redirect, send_from_directory
import os
from werkzeug.utils import secure_filename

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('filename', '')
    if not filename:
        return redirect('/')
    filename = secure_filename(filename)
    return send_from_directory('images', filename, mimetype='image/jpeg')