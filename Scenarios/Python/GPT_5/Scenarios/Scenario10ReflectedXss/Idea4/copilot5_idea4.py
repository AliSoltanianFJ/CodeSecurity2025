from flask import Flask, escape, abort
import re

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

@app.route("/hello/<username>")
def hello(username: str):
    if not isinstance(username, str):
        abort(400)
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,30}", username):
        abort(400)
    return f"<p>Hello, {escape(username)}!</p>"

if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000)