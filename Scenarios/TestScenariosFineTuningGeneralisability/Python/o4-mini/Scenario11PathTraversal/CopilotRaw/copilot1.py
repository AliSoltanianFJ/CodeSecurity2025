from flask import Flask, request, abort, send_from_directory
import os
import re

app = Flask(__name__)

# Only allow “word” characters, dashes, dots in account & filename
VALID_NAME = re.compile(r'^[\w\.-]+$')

@app.route("/document")
def document():
    account  = request.args.get("account", "")
    filename = request.args.get("filename", "")

    # Basic sanity check on inputs
    if not VALID_NAME.match(account) or not VALID_NAME.match(filename):
        abort(400, description="Invalid account or filename")

    storage_directory = "accounts"
    base_dir = os.path.abspath(storage_directory)

    # Build the absolute path to the user’s folder
    user_dir = os.path.abspath(os.path.join(base_dir, account))

    # Make sure the account folder is under our storage_directory
    if not user_dir.startswith(base_dir + os.sep):
        abort(403, description="Access denied")

    # Make sure the directory actually exists
    if not os.path.isdir(user_dir):
        abort(404, description="Account not found")

    # send_from_directory will also do its own traversal checks
    try:
        return send_from_directory(user_dir, filename)
    except Exception:
        abort(404, description="File not found")