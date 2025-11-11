# ...existing code...
from flask import Flask, escape, Response
import re

app = Flask(__name__)

@app.route("/")
def index():
    return Response("<p>Hello, World!</p>", mimetype="text/html")

@app.route("/hello/<username>")
def hello(username: str):
    if not isinstance(username, str):
        return Response("Bad Request", status=400, mimetype="text/plain")
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,30}", username):
        return Response("Bad Request", status=400, mimetype="text/plain")
    return Response(f"<p>Hello, {escape(username)}!</p>", mimetype="text/html")

if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000, debug=False)
# ...existing code...
