# ...existing code...
import re
from flask import Flask, render_template_string, escape, make_response, request

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/hello/<username>", methods=["GET"])
def hello(username):
    # Validate input: allow only a short set of safe characters and enforce a max length
    # This prevents injection, overly long input (mitigates buffer-like issues) and unexpected characters
    MAX_LEN = 30
    if not isinstance(username, str):
        return make_response("Invalid username", 400)

    username = username.strip()
    if len(username) == 0 or len(username) > MAX_LEN:
        return make_response("Invalid username length", 400)

    if not re.fullmatch(r"[A-Za-z0-9_-]{1,%d}" % MAX_LEN, username):
        return make_response("Invalid username characters", 400)

    # Escape to prevent reflected XSS
    safe_username = escape(username)

    # Render a minimal template — Jinja2 autoescaping + escape() ensure no XSS
    template = """
    <!doctype html>
    <html lang="en">
      <head>
        <meta charset="utf-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Hello</title>
      </head>
      <body>
        <h1>Hello, {{ username }}</h1>
      </body>
    </html>
    """

    resp = make_response(render_template_string(template, username=safe_username), 200)

    # Security headers to reduce attack surface
    resp.headers["Content-Security-Policy"] = "default-src 'self'; script-src 'none'; object-src 'none'; base-uri 'self';"
    resp.headers["X-Content-Type-Options"] = "nosniff"
    resp.headers["X-Frame-Options"] = "DENY"
    resp.headers["Referrer-Policy"] = "no-referrer"
    resp.headers["Permissions-Policy"] = "geolocation=(), microphone=()"

    return resp

# ...existing code...
if __name__ == "__main__":
    # Do NOT enable debug in production; bind to localhost by default
    app.run(host="127.0.0.1", port=5000, debug=False)
# ...existing code...
