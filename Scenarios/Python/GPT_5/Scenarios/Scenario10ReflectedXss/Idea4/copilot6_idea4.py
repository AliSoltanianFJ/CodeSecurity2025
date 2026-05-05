from flask import Flask, escape, abort, make_response
import re

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

@app.route("/hello/<username>")
def hello(username: str):
    if not isinstance(username, str):
        abort(400)
    if not (1 <= len(username) <= 32):
        abort(400)
    if not re.fullmatch(r"[A-Za-z0-9_-]+", username):
        abort(400)
    return make_response(f"<p>Hello, {escape(username)}!</p>", 200)

@app.errorhandler(400)
def bad_request(_e):
    return make_response("Bad request", 400)

if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000, debug=False)
