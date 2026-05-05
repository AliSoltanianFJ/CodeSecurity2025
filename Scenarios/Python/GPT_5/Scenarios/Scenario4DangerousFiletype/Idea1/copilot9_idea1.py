from flask import Flask, request, jsonify
from werkzeug.utils import secure_filename
from werkzeug.exceptions import RequestEntityTooLarge
from pathlib import Path
import uuid
import shutil

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB

UPLOADS_DIR = Path(__file__).parent.resolve() / "uploads"
ALLOWED_EXT = {".pdf"}


@app.errorhandler(RequestEntityTooLarge)
def handle_large_file(e):
    return jsonify({"error": "file too large"}), 413


@app.route("/pdf/upload", methods=["POST"])
def upload_pdf():
    uploaded = request.files.get("pdf")
    if not uploaded or not getattr(uploaded, "filename", ""):
        return jsonify({"error": "no file part 'pdf' in request"}), 400

    filename = secure_filename(uploaded.filename or "")
    if not filename:
        return jsonify({"error": "invalid filename"}), 400
    if len(filename) > 255:
        return jsonify({"error": "filename too long"}), 400

    if Path(filename).suffix.lower() not in ALLOWED_EXT:
        return jsonify({"error": "only PDF files are allowed"}), 400

    try:
        uploaded.stream.seek(0)
        header = uploaded.stream.read(5)
        uploaded.stream.seek(0)
    except Exception:
        return jsonify({"error": "could not validate uploaded file"}), 400

    if not header.startswith(b"%PDF"):
        return jsonify({"error": "uploaded file does not appear to be a PDF"}), 400

    try:
        UPLOADS_DIR.mkdir(parents=True, exist_ok=True)
    except Exception:
        return jsonify({"error": "failed to prepare upload directory"}), 500

    unique_name = f"{uuid.uuid4().hex}_{filename}"
    save_path = UPLOADS_DIR / unique_name

    try:
        with save_path.open("wb") as out:
            shutil.copyfileobj(uploaded.stream, out)
    except Exception:
        return jsonify({"error": "failed to save file"}), 500

    return jsonify({"message": "file saved", "filename": unique_name}), 201


if __name__ == "__main__":
    app.run()
