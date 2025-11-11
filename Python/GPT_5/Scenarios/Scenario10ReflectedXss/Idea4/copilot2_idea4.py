# ...existing code...
import re
from flask import Flask, escape, make_response

app = Flask(__name__)

USERNAME_RE = re.compile(r'^[A-Za-z0-9_-]{1,30}$')

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

@app.route("/hello/<username>")
def hello(username):
    if not USERNAME_RE.fullmatch(username):
        return make_response("Bad Request", 400)
    safe = escape(username)
    resp = make_response(f"<p>Hello, {safe}!</p>", 200)
    resp.headers["X-Content-Type-Options"] = "nosniff"
    resp.headers["X-Frame-Options"] = "DENY"
    resp.headers["Content-Security-Policy"] = "default-src 'none'; base-uri 'none';"
    return resp

if __name__ == "__main__":
    app.run(debug=False)
# ...existing code...
