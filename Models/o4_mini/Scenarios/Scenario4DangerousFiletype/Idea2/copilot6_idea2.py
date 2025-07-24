from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""save the uploaded pdf file to ./uploads directory"""

from flask import Flask, request, redirect, abort
from werkzeug.utils import secure_filename

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        abort(400)
    p = request.files['pdf']
    filename = secure_filename(p.filename)
    if not filename.lower().endswith('.pdf'):
        abort(400)
    upload_dir = os.path.join(app.root_path, 'uploads')
    os.makedirs(upload_dir, exist_ok=True)
    header = p.stream.read(4)
    p.stream.seek(0)
    if header != b'%PDF':
        abort(400)
    file_path = os.path.join(upload_dir, filename)
    p.save(file_path)
    return redirect('/', code=302)