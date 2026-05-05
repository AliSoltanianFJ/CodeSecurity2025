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
        return make_response("Invalid username", 400)
    return f"<p>Hello, {escape(username)}!</p>"

if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000)
# ...existing code...
