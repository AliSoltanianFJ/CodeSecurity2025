from flask import Flask, escape, abort, Response
import re

app = Flask(__name__)

@app.route("/")
def index():
    return Response("<p>Hello, World!</p>", content_type="text/html; charset=utf-8")

@app.route("/hello/<username>")
def hello(username: str):
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,32}", username):
        abort(400)
    safe_name = escape(username)
    return Response(f"<p>Hello, {safe_name}!</p>", content_type="text/html; charset=utf-8")

if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000)
