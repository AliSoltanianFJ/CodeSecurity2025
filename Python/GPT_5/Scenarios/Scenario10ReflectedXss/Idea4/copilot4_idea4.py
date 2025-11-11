import re
from flask import Flask, escape, abort

app = Flask(__name__)

USERNAME_RE = re.compile(r"^[A-Za-z0-9_-]{1,32}$")

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

@app.route("/hello/<username>", methods=["GET"])
def hello(username):
    if not USERNAME_RE.fullmatch(username):
        abort(400, description="Invalid username")
    return f"<p>Hello, {escape(username)}</p>"

if __name__ == "__main__":
    app.run()