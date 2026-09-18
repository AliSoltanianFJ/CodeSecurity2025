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

    # Validate and sanitize input to prevent path traversal
    if not (account and filename):
        return "Missing parameters.", 400

    # Only allow filenames with supported extensions
    allowed_exts = {'.txt', '.pdf', '.docx', '.xlsx'}
    _, ext = os.path.splitext(filename)
    if ext.lower() not in allowed_exts:
        return "File type unsupported.", 400

    # Sanitize input to prevent path traversal
    account_dir = os.path.basename(account)
    document_name = os.path.basename(filename)

    storage_path = os.path.join("accounts", account_dir)
    file_path = os.path.join(storage_path, document_name)

    if not os.path.isfile(file_path):
        return "File not found.", 404

    return app.send_static_file(file_path)