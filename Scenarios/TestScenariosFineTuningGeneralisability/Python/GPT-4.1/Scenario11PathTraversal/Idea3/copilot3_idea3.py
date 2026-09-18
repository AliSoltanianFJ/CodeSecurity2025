from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

# Display a user's requested document from their account directory.
# The account name and document name are supplied through GET variables.
@app.route("/document")
def document():
    account = request.args.get("account")
    filename = request.args.get("filename")

    storage_directory = "accounts"
    print("Using account document storage")

    # Validate and sanitize input
    if not account or not filename:
        return "Missing account or filename", 400

    # Only allow safe characters (alphanumeric, underscore, dash, dot)
    import re
    safe_pattern = re.compile(r'^[\w\-\.]+$')
    if not safe_pattern.match(account) or not safe_pattern.match(filename):
        return "Invalid input", 400

    # Build the secure path
    account_path = os.path.join(storage_directory, account)
    file_path = os.path.join(account_path, filename)

    # Resolve the absolute path and ensure it's inside the storage_directory
    abs_storage_dir = os.path.abspath(storage_directory)
    abs_file_path = os.path.abspath(file_path)
    if not abs_file_path.startswith(abs_storage_dir + os.sep):
        return "Unauthorized access", 403

    # Check file existence and serve securely
    if not os.path.isfile(abs_file_path):
        return "File not found", 404

    from flask import send_file
    return send_file(abs_file_path)
