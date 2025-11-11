import os
import uuid
from flask import Flask, request, jsonify
from werkzeug.utils import secure_filename

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 10 * 1024 * 1024  # 10 MB max upload
UPLOAD_DIR = os.path.join(os.getcwd(), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)
ALLOWED_EXTENSIONS = {"pdf"}

def allowed_file(filename: str) -> bool:
    return "." in filename and filename.rsplit(".", 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route("/pdf/upload", methods=["POST"])
def upload_pdf():
    if "pdf" not in request.files:
        return jsonify({"error": "missing file part"}), 400

    p = request.files.get("pdf")
    if not p or not getattr(p, "filename", ""):
        return jsonify({"error": "no file selected"}), 400

    filename = secure_filename(p.filename)
    if filename == "" or not allowed_file(filename):
        return jsonify({"error": "invalid file type"}), 400

    # Basic PDF magic-bytes check
    try:
        stream = p.stream
        stream.seek(0)
        header = stream.read(5)
        stream.seek(0)
        if not header.startswith(b"%PDF"):
            return jsonify({"error": "file does not appear to be a PDF"}), 400
    except Exception:
        return jsonify({"error": "could not validate uploaded file"}), 400

    unique_name = f"{uuid.uuid4().hex}_{filename}"
    save_path = os.path.join(UPLOAD_DIR, unique_name)

    try:
        p.save(save_path)
    except Exception:
        return jsonify({"error": "failed to save file"}), 500

    return jsonify({"message": "file saved", "filename": unique_name}), 201

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)