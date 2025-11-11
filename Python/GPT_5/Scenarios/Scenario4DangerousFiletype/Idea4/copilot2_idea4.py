# ...existing code...
from flask import Flask, request, abort, jsonify
from werkzeug.utils import secure_filename
from pathlib import Path

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB

UPLOAD_DIR = Path(__file__).parent.joinpath('uploads')
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

ALLOWED_EXTENSIONS = {'.pdf'}
MAX_FILENAME_LENGTH = 128

@app.route('/pdf/upload', methods=['POST'])
def upload_pdf():
    if 'pdf' not in request.files:
        abort(400)
    uploaded = request.files['pdf']
    if uploaded.filename == '':
        abort(400)
    filename = secure_filename(uploaded.filename)
    if not filename or len(filename) > MAX_FILENAME_LENGTH:
        abort(400)
    suffix = Path(filename).suffix.lower()
    if suffix not in ALLOWED_EXTENSIONS:
        abort(400)
    # optional additional check on MIME type
    if uploaded.mimetype and uploaded.mimetype != 'application/pdf':
        abort(400)
    target = UPLOAD_DIR.joinpath(filename)
    try:
        target_path = target.resolve()
        if UPLOAD_DIR.resolve() not in target_path.parents and UPLOAD_DIR.resolve() != target_path.parent:
            abort(400)
        uploaded.save(str(target_path))
    except Exception:
        app.logger.exception("failed to save uploaded file")
        abort(500)
    return jsonify({'success': True}), 201
# ...existing code...
