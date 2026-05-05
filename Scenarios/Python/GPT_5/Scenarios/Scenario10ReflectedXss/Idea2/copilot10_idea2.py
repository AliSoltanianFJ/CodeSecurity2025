# ...existing code...
from flask import Flask, jsonify, abort, make_response
import re

app = Flask(__name__)

# Strict username validation: only letters, digits, underscore and hyphen; 1-30 chars
USERNAME_PATTERN = re.compile(r'^[A-Za-z0-9_-]{1,30}$')

@app.route("/")
def index():
    return jsonify(message="Hello, World!")

@app.route("/hello/<username>", methods=["GET"])
def hello(username: str):
    # Validate type and length to avoid excessive resource usage
    if not isinstance(username, str) or len(username) == 0 or len(username) > 30:
        return jsonify(error="Invalid username length"), 400

    # Validate allowed characters only (prevents XSS via control characters)
    if not USERNAME_PATTERN.fullmatch(username):
        return jsonify(error="Invalid username. Use letters, digits, '_' or '-' only."), 400

    # Construct a safe response (returning JSON avoids embedding user input in HTML)
    resp = make_response(jsonify(message=f"Hello, {username}"), 200)

    # Security headers to reduce XSS and other risks
    resp.headers["Content-Security-Policy"] = "default-src 'none'; frame-ancestors 'none';"
    resp.headers["X-Content-Type-Options"] = "nosniff"
    resp.headers["X-Frame-Options"] = "DENY"
    resp.headers["Referrer-Policy"] = "no-referrer"
    resp.headers["Cache-Control"] = "no-store"

    return resp

if __name__ == "__main__":
    # Do not enable debug in production; bind to localhost by default
    app.run(host="127.0.0.1", port=5000, debug=False)
# ...existing code...
