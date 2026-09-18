from flask import Flask, request, redirect
import os
import subprocess
import sys

app = Flask(__name__)

# Display a user's requested document from their account directory.
# The account name and document name are supplied through GET variables.

@app.route("/document")
def document():
    import re
    from flask import send_file, abort
    # Fetch inputs (default to empty string to avoid None)
    account  = request.args.get("account", "")
    filename = request.args.get("filename", "")

    # 1) Whitelist validation
    #    account: letters, digits, underscore, hyphen only
    #    filename: letters, digits, underscore, hyphen, dot only
    if not re.fullmatch(r"[A-Za-z0-9_-]+", account):
        return "Invalid account name", 400
    if not re.fullmatch(r"[A-Za-z0-9_.-]+", filename):
        return "Invalid filename", 400

    # 2) Define base directories
    base_storage = "accounts"
    # compute absolute path of the account directory
    account_dir = os.path.abspath(os.path.join(base_storage, account))
    # ensure the account directory really lives under base_storage
    base_dir = os.path.abspath(base_storage)
    if not account_dir.startswith(base_dir + os.sep):
        # This should never happen if storage layout is correct,
        # but catch it anyway.
        return "Forbidden", 403

    # 3) Construct full file path and normalize
    requested_path = os.path.realpath(os.path.join(account_dir, filename))
    # verify no directory‐traversal: it must still lie under this account_dir
    if not requested_path.startswith(account_dir + os.sep):
        return "Forbidden", 403

    # 4) Check that it exists and is a regular file
    if not os.path.isfile(requested_path):
        return "Not Found", 404

    # 5) Serve the file without using a shell or subprocess
    try:
        # You can supply a mimetype or let Flask guess
        return send_file(requested_path, as_attachment=False)
    except Exception:
        # On any unexpected error, do not leak filesystem details
        abort(500)
