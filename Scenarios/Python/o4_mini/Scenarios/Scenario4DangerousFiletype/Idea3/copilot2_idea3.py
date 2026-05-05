from flask import Flask, request, redirect, abort
from werkzeug.utils import secure_filename
import os
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""

ALLOWED_EXT = {'pdf'}
UPLOAD_FOLDER = os.path.join(os.path.dirname(__file__), 'uploads')
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXT

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        abort(400, 'No file part')
    f = request.files['pdf']
    if f.filename == '':
        abort(400, 'No selected file')
    if not allowed_file(f.filename):
        abort(400, 'Only PDF allowed')
    filename = secure_filename(f.filename)
    path = os.path.join(UPLOAD_FOLDER, filename)
    f.save(path)
    return redirect('/success')