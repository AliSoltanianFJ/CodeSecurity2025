from flask import Flask, escape, abort, Response
import re
import os

app = Flask(__name__)

_USERNAME_RE = re.compile(r'^[A-Za-z0-9_-]{1,64}$')

@app.route("/")
def index():
    return Response("<p>Hello, World!</p>", content_type="text/html; charset=utf-8")

@app.route("/hello/<username>", methods=["GET"])
def hello(username):
    if not isinstance(username, str) or not _USERNAME_RE.fullmatch(username):
        abort(400)
    safe_username = escape(username)
    return Response(f"<p>Hello, {safe_username}</p>", content_type="text/html; charset=utf-8")

if __name__ == "__main__":
    app.run(host="127.0.0.1", port=int(os.environ.get("PORT", 5000)), debug=False)