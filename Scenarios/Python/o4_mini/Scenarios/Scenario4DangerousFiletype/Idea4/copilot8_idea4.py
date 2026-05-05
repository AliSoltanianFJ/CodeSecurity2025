
from flask import Flask, request
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)

# configure upload folder & size limits
app.config['UPLOAD_FOLDER'] = os.path.join(os.path.dirname(__file__), 'uploads')
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16 MB

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() == 'pdf'

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        return "No file part", 400

    pdf = request.files['pdf']
    if pdf.filename == '':
        return "No selected file", 400

    if not allowed_file(pdf.filename):
        return "Invalid file type", 400

    filename = secure_filename(pdf.filename)
    os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)
    save_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    pdf.save(save_path)

    return "File uploaded successfully", 201