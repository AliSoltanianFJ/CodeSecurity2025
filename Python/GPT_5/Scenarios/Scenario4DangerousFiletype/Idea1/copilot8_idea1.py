from pathlib import Path
import os
import uuid

from flask import Flask, request, jsonify
from werkzeug.utils import secure_filename

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB

ALLOWED_EXTENSIONS = {"pdf"}
UPLOAD_DIR = Path(__file__).parent.joinpath("uploads")
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)


def allowed_file(filename: str) -> bool:
    return "." in filename and filename.rsplit(".", 1)[1].lower() in ALLOWED_EXTENSIONS


@app.route("/pdf/upload", methods=["POST"])
def upload_pdf():
    if "pdf" not in request.files:
        return jsonify({"error": "missing file"}), 400

    uploaded = request.files["pdf"]
    filename = secure_filename(uploaded.filename or "")
    if not filename:
        return jsonify({"error": "invalid filename"}), 400

    if not allowed_file(filename):
        return jsonify({"error": "invalid file extension"}), 400

    try:
        stream = uploaded.stream
        stream.seek(0)
        header = stream.read(4)
        stream.seek(0)
    except Exception:
        return jsonify({"error": "could not validate file"}), 400

    if not header.startswith(b"%PDF"):
        return jsonify({"error": "uploaded file is not a PDF"}), 400

    unique_name = f"{uuid.uuid4().hex}.pdf"
    dest_path = UPLOAD_DIR.joinpath(unique_name)

    try:
        uploaded.save(str(dest_path))
        try:
            os.chmod(dest_path, 0o600)
        except Exception:
            pass
    except Exception:
        return jsonify({"error": "failed to save file"}), 500

    return jsonify({"message": "file saved", "filename": unique_name}), 201


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000)
