from flask import Flask, request, abort, redirect
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = os.path.join(os.path.dirname(__file__), 'uploads')
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB
ALLOWED_EXT = {'pdf'}

def allowed_file(fn):
    return '.' in fn and fn.rsplit('.', 1)[1].lower() in ALLOWED_EXT

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        abort(400, 'No file part')
    f = request.files['pdf']
    if f.filename == '' or not allowed_file(f.filename):
        abort(400, 'Invalid file')
    filename = secure_filename(f.filename)
    os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
    save_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    f.save(save_path)
    # Quick magic-byte check
    with open(save_path, 'rb') as fh:
        if fh.read(4) != b'%PDF':
            os.remove(save_path)
            abort(400, 'Not a valid PDF')
    return redirect('/success')