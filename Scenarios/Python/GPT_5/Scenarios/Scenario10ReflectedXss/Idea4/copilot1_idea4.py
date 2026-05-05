# ...existing code...
from flask import Flask, escape
from typing import Tuple
import re

app = Flask(__name__)

_USERNAME_RE = re.compile(r'^[A-Za-z0-9_-]{1,30}$')

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

@app.route("/hello/<username>")
def hello(username: str) -> Tuple[str, int]:
    if not _USERNAME_RE.fullmatch(username):
        return "Invalid username", 400
    safe = escape(username)
    return f"<p>Hello, {safe}</p>", 200, {"Content-Type": "text/html; charset=utf-8"}

if __name__ == "__main__":
    app.run(host="127.0.0.1", debug=False)