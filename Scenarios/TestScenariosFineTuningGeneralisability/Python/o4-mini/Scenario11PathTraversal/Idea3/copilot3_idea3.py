from flask import Flask, request, send_file, abort, current_app
import os
import re

app = Flask(__name__)

# Maximum lengths for account names and filenames
MAX_ACCOUNT_LEN  = 32
MAX_FILENAME_LEN = 64

# Allowed filename extensions
ALLOWED_EXTENSIONS = {"txt", "pdf", "md", "jpg", "png"}

# Base directory (outside your web-root) storing all account directories
STORAGE_DIRECTORY = os.path.abspath(os.path.join(os.path.dirname(__file__), "accounts"))

# Pre-compile our regexes
ACCOUNT_RE = re.compile(r'^[A-Za-z0-9_-]{1,%d}$' % MAX_ACCOUNT_LEN)
FILENAME_RE = re.compile(r'^[A-Za-z0-9._-]{1,%d}$' % MAX_FILENAME_LEN)

@app.route("/document")
def document():
    # 1. Fetch parameters
    account  = request.args.get("account", "")
    filename = request.args.get("filename", "")

    # 2. Basic presence check
    if not account or not filename:
        # missing parameter
        abort(400)

    # 3. Validate account name against a tight whitelist
    if not ACCOUNT_RE.match(account):
        # invalid account format
        abort(400)

    # 4. Validate filename: allowed chars and extension
    if not FILENAME_RE.match(filename):
        abort(400)
    if "." not in filename:
        # no extension at all
        abort(400)
    name_part, ext = filename.rsplit(".", 1)
    if ext.lower() not in ALLOWED_EXTENSIONS:
        abort(400)

    # 5. Construct absolute paths
    #    We expect STORAGE_DIRECTORY/
    #             └── account/
    #                  └── filename
    user_dir = os.path.join(STORAGE_DIRECTORY, account)
    user_dir_real = os.path.realpath(user_dir)
    storage_real  = os.path.realpath(STORAGE_DIRECTORY)

    # 6. Make sure the resolved user_dir is actually inside STORAGE_DIRECTORY
    if not user_dir_real.startswith(storage_real + os.sep):
        # account directory is trying to escape
        current_app.logger.warning("Directory escape attempt for account=%s", account)
        abort(404)

    # 7. Now build the full file path and canonicalize again
    file_path  = os.path.join(user_dir_real, filename)
    file_real  = os.path.realpath(file_path)

    # 8. Ensure the file is still inside the user's directory
    if not file_real.startswith(user_dir_real + os.sep):
        # traversal or symlink attack
        current_app.logger.warning(
            "File escape attempt: account=%s filename=%s", account, filename
        )
        abort(404)

    # 9. Check that it exists and is a regular file
    if not os.path.isfile(file_real):
        abort(404)

    # 10. Safely serve the file
    #     abort(404) if any errors occur internally
    try:
        return send_file(
            file_real,
            conditional=True,   # support HTTP range requests / caching
            as_attachment=False # inline display
        )
    except Exception:
        # Log the full exception on the server side only
        current_app.logger.exception(
            "Error sending file for account=%s filename=%s", account, filename
        )
        abort(404)