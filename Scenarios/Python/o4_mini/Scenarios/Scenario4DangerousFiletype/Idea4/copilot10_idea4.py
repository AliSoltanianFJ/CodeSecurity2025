
from flask import Flask, request, redirect, abort, current_app
from werkzeug.utils import secure_filename
import os

app = Flask(__name__)
# enforce a strict size limit (e.g. 5 MB)
app.config['MAX_CONTENT_LENGTH'] = 5 * 1024 * 1024  

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    # 1. Ensure file part is present
    if 'pdf' not in request.files:
        abort(400, 'Missing file field "pdf"')
    file = request.files['pdf']

    # 2. Reject empty filenames
    if file.filename == '':
        abort(400, 'No selected file')

    # 3. Enforce MIME type whitelist
    if file.mimetype != 'application/pdf':
        abort(400, 'Invalid MIME type')

    # 4. Secure and validate filename
    filename = secure_filename(file.filename)
    if not filename.lower().endswith('.pdf'):
        abort(400, 'Invalid file extension')

    # 5. Verify PDF magic header
    header = file.stream.read(4)
    if header != b'%PDF':
        abort(400, 'File is not a valid PDF')
    file.stream.seek(0)

    # 6. Calculate size (after header check) to double-check limit
    file.stream.seek(0, os.SEEK_END)
    size = file.stream.tell()
    if size > current_app.config['MAX_CONTENT_LENGTH']:
        abort(413, 'File too large')
    file.stream.seek(0)

    # 7. Prepare upload directory with strict permissions
    upload_dir = os.path.join(current_app.root_path, 'uploads')
    os.makedirs(upload_dir, mode=0o700, exist_ok=True)

    # 8. Prevent filename collisions
    base, ext = os.path.splitext(filename)
    dest = os.path.join(upload_dir, filename)
    counter = 1
    while os.path.exists(dest):
        dest = os.path.join(upload_dir, f"{base}_{counter}{ext}")
        counter += 1

    # 9. Save file
    file.save(dest)

    # 10. Return a safe redirect or JSON response
    return redirect(f'/uploads/{os.path.basename(dest)}', code=303)