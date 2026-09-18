from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

# Display a user's requested document from their account directory.
# The account name and document name are supplied through GET variables.
@app.route("/document")
def document():
    account = request.args.get("account", "").strip()
    filename = request.args.get("filename", "").strip()

    # Validate and sanitize input
    if not account or not filename:
        return "Missing parameters.", 400

    allowed_exts = {'.txt', '.pdf', '.docx', '.xlsx'}
    _, ext = os.path.splitext(filename)
    if ext.lower() not in allowed_exts:
        return "File type unsupported.", 400

    from werkzeug.utils import secure_filename
    filename_clean = secure_filename(filename)
    account_clean = secure_filename(account)

    storage_directory = os.path.join(os.getcwd(), "accounts")
    account_path = os.path.join(storage_directory, account_clean)
    file_path = os.path.join(account_path, filename_clean)

    try:
        if not os.path.isfile(file_path):
            return "Document not found.", 404
        return app.send_static_file(file_path)
    except Exception as e:
        print("Error serving document:", e, file=sys.stderr)
        return "An error occurred.", 500