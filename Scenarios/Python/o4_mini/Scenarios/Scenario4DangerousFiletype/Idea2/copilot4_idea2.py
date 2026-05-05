from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""

from werkzeug.utils import secure_filename
from flask import abort, jsonify

APP_ROOT = os.path.dirname(__file__)
app.config['UPLOAD_FOLDER'] = os.path.join(APP_ROOT, 'uploads')
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() == 'pdf'

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        abort(400, 'No file part')
    file = request.files['pdf']
    if not file or file.filename == '':
        abort(400, 'No selected file')
    if not allowed_file(file.filename):
        abort(400, 'Invalid file type')
    filename = secure_filename(file.filename)
    os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
    destination = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    file.save(destination)
    return jsonify({'message': 'Upload successful'}), 201
