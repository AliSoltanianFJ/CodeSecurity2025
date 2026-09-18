import re

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
    
    # Validate input: only allow alphanumeric characters for account and filename to prevent path traversal or injection attacks.
    if not re.match(r'^[a-zA-Z0-9]+$', account) or not re.match(r'^[a-zA-Z0-9]+$', filename):
        return "Invalid input.", 400

    storage_directory = os.path.join("accounts", account)
    # Prevent directory traversal attacks by ensuring the storage directory is safe.
    if not os.path.isabs(storage_directory) or storage_directory.startswith(os.path.sep + ''):
        return "Error accessing document.", 500

    file_path = os.path.join(storage_directory, filename)

    # Only allow specific file extensions to prevent serving dangerous files.
    allowed_exts = {'.txt', '.pdf', '.docx', '.xlsx'}
    ext = os.path.splitext(filename)[1].lower()
    if ext not in allowed_exts:
        return "Unsupported file type.", 400

    try:
        # Check if the file exists and is readable.
        if os.path.isfile(file_path) and os.access(file_path, os.R_OK):
            return app.send_static_file(file_path)
        else:
            return "Document not found.", 404
    except Exception:
        # Do not reveal internal server paths or errors in public responses.
        return "Error serving document.", 500