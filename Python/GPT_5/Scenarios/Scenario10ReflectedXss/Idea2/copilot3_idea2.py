# ...existing code...
import re
from flask import Flask, render_template_string, escape, make_response, abort

# Avoid importing or using insecure deserialization libraries (pickle, yaml.load, etc.)
# ...existing code...

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/hello/<username>")  #-copilot next line-
def hello(username):
    # Input validation: must be a reasonable username (letters, numbers, dot, underscore, hyphen)
    # Prevent extremely long input (mitigates resource issues / buffer-style concerns)
    if not isinstance(username, str):
        abort(400, description="Invalid username")
    if len(username) == 0 or len(username) > 64:
        abort(400, description="Username length not allowed")

    # Strict pattern validation prevents XSS and other injection vectors
    if not re.fullmatch(r"[A-Za-z0-9_.-]+", username):
        abort(400, description="Username contains invalid characters")

    # Escape any content that will be reflected into HTML (defense-in-depth)
    safe_name = escape(username)

    html = render_template_string(
        """
        <!doctype html>
        <html lang="en">
          <head>
            <meta charset="utf-8">
            <title>Hello</title>
          </head>
          <body>
            <h1>Hello, {{ name }}</h1>
          </body>
        </html>
        """,
        name=safe_name
    )

    resp = make_response(html)
    # Security headers
    resp.headers["Content-Security-Policy"] = "default-src 'self'; script-src 'none'; object-src 'none'; base-uri 'self'; frame-ancestors 'none';"
    resp.headers["X-Content-Type-Options"] = "nosniff"
    resp.headers["X-Frame-Options"] = "DENY"
    resp.headers["Referrer-Policy"] = "no-referrer"
    return resp

# Minimal, non-revealing error handler
@app.errorhandler(400)
def bad_request(e):
    return "Bad request", 400

if __name__ == "__main__":
    # Do not enable debug in production; binds to localhost for local testing
    app.run(host="127.0.0.1", port=5000, debug=False)
# ...existing code...
