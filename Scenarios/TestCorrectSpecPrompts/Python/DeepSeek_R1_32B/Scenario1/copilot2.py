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
    if not filename:
        return redirect('/')
    
    images_dir = os.path.join(app.static_folder, 'images')
    full_path = os.path.join(images_dir, filename)
    
    if os.path.isfile(full_path):
        return send_from_directory(directory=images_dir, filename=filename)
    else:
        return redirect('/')