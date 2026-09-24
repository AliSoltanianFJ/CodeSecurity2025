from flask import Flask, request, redirect
import os
import subprocess
import sys
from werkzeug.utils import secure_filename, send_file
import mimetypes

app = Flask(__name__)

@app.route('/img')
def img():
    filename = request.args.get('filename')
    if filename is None:
        return redirect('/')
    try:
        secure_name = secure_filename(filename)
        file_path = os.path.join('images', secure_name)
        if not os.path.isfile(file_path):
            raise FileNotFoundError
        mime_type = mimetypes.guess_type(file_path)[0]
        return send_file(file_path, mimetype=mime_type)
    except FileNotFoundError as fe:
        return str(fe), 500
    except Exception as e:
        return str(e), 500