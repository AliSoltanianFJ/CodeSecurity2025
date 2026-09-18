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

    # Validate input
    if not account or not filename:
        return "Missing account or filename", 400

    # Only allow safe characters (alphanumeric, _, -, .)
    import re
    safe_pattern = re.compile(r'^[\w\-. ]+$')
    if not safe_pattern.match(account) or not safe_pattern.match(filename):
        return "Invalid characters in input", 400

    # Build safe file path
    user_dir = os.path.join(storage_directory, account)
    file_path = os.path.join(user_dir, filename)

    # Prevent directory traversal
    user_dir_abs = os.path.abspath(user_dir)
    file_path_abs = os.path.abspath(file_path)
    if not file_path_abs.startswith(user_dir_abs + os.sep):
        return "Invalid file path", 400

    # Check if file exists
    if not os.path.isfile(file_path_abs):
        return "File not found", 404

    # Serve file securely
    from flask import send_file
    return send_file(file_path_abs)

# Notes:
# - Only allows safe characters in account and filename.
# - Prevents directory traversal.
# - Uses send_file for secure file serving.
# - Returns errors for invalid input or missing files.
