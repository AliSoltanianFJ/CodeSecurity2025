from flask import Flask, request, abort, send_from_directory
from werkzeug.utils import safe_join
import os
import re

app = Flask(__name__)

# Precompile regexes for performance
ACCOUNT_REGEX = re.compile(r'^[A-Za-z0-9_-]+$')
FILENAME_REGEX = re.compile(r'^[A-Za-z0-9._-]+$')

# Base directory under which all user accounts live.  Must be outside
# of your web‐root and not writable by untrusted processes.
STORAGE_DIRECTORY = os.path.abspath("accounts")

@app.route("/document")
def document():
    # 1) Fetch and strip inputs
    account = (request.args.get("account") or "").strip()
    filename = (request.args.get("filename") or "").strip()

    # 2) Basic presence check
    if not account or not filename:
        # 400 Bad Request if either parameter is missing or empty
        abort(400, description="Missing account or filename parameter")

    # 3) Validate allowed characters only
    if not ACCOUNT_REGEX.fullmatch(account):
        abort(400, description="Invalid account name")
    if not FILENAME_REGEX.fullmatch(filename):
        abort(400, description="Invalid filename")

    # 4) Construct a secure full path using Werkzeug's safe_join
    #    safe_join will return None or raise if the result would escape STORAGE_DIRECTORY
    try:
        user_file_path = safe_join(STORAGE_DIRECTORY, account, filename)
    except Exception:
        # safe_join raises a NotFound exception if any path segment is unsafe
        abort(404)

    # 5) Double-check that the resulting path is under STORAGE_DIRECTORY
    #    (belt-and-suspenders—we already used safe_join, but worth verifying)
    if not user_file_path:
        abort(404)
    abs_base = STORAGE_DIRECTORY + os.sep
    abs_target = os.path.abspath(user_file_path)
    if not abs_target.startswith(abs_base):
        abort(404)

    # 6) Ensure the account directory exists and is indeed a directory
    account_dir = os.path.dirname(abs_target)
    if not os.path.isdir(account_dir):
        abort(404)

    # 7) Ensure the target is a file and not a symlink to somewhere else
    if not os.path.isfile(abs_target) or os.path.islink(abs_target):
        abort(404)

    # 8) (Optional) Enforce a whitelist of extensions
    allowed_ext = {'.txt', '.pdf', '.md'}
    _, ext = os.path.splitext(filename)
    if ext.lower() not in allowed_ext:
        abort(403, description="File type not allowed")

    # 9) Serve the file directly from the account directory
    #    as a non-attachment so it renders in the browser if possible
    return send_from_directory(account_dir, filename,
                               as_attachment=False,
                               conditional=True)

# You can add more routes or app.run() as you like...