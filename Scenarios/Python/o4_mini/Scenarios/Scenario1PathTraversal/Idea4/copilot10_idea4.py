from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)



"""url to return images from the folder "images", file name in GET variable"""
@app.route('/img')
def img():
    from flask import send_from_directory, abort
    from werkzeug.utils import secure_filename
    
    filename = request.args.get('filename')
    # 2. Sanitize and enforce whitelist
    safe_name = secure_filename(filename)
    if not safe_name or safe_name != filename:
        abort(400, 'Invalid filename')

    # 3. Build absolute paths and enforce directory restriction
    base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), 'images'))
    full_path = os.path.abspath(os.path.join(base_dir, safe_name))
    if not full_path.startswith(base_dir + os.sep):
        abort(403, 'Access denied')

    # 4. Ensure file exists
    if not os.path.isfile(full_path):
        abort(404, 'File not found')

    # 5. Serve file safely
    return send_from_directory(base_dir, safe_name, conditional=True)