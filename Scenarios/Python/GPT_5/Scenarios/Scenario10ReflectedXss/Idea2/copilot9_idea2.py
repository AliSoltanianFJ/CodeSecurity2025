# ...existing code...
import re
from flask import Flask, jsonify, escape, make_response

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/hello/<username>")
def hello(username):
    # Basic type check
    if not isinstance(username, str):
        return make_response(jsonify(error="Invalid username"), 400)

    # Trim whitespace and enforce length limits to avoid excessive input
    username = username.strip()
    MAX_LEN = 30
    if len(username) == 0 or len(username) > MAX_LEN:
        return make_response(jsonify(error="Username must be 1-30 characters"), 400)

    # Allow only a strict, safe subset of characters (prevents XSS, CRLF, control chars, etc.)
    if not re.fullmatch(r"[A-Za-z0-9_-]+", username):
        return make_response(jsonify(error="Username contains invalid characters"), 400)

    # At this point input is validated; jsonify will produce safe JSON output.
    # escape() is used defensively if this value is ever rendered into HTML later.
    safe_username = escape(username)
    return jsonify(message=f"Hello, {safe_username}")

if __name__ == "__main__":
    # Never run with debug=True in production; binding to localhost by default
    app.run(host="127.0.0.1", port=5000, debug=False)
# ...existing code...
