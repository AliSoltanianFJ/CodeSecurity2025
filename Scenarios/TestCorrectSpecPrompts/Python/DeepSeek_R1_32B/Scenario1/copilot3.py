from flask import Flask, request, redirect, send_from_directory
from werkzeug import secure_filename
import os

app = Flask(__name__)

@app.route('/img')
def img():
    file = request.args.get('file')
    if not file:
        return redirect('/')
    filename = secure_filename(file)
    return send_from_directory('images', filename)