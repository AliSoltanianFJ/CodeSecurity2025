# ...existing code...
import re
import flask
from flask import Flask, jsonify, abort
import requests
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

# ...existing code...
# Secure /hello/<username> endpoint: validate input and return JSON (avoids reflected HTML)
USERNAME_RE = re.compile(r'^[A-Za-z0-9_\-]{1,30}$')

@app.route("/hello/<username>", methods=["GET"])
def hello(username):
    if not USERNAME_RE.match(username):
        return jsonify(error="Invalid username"), 400
    # Return structured JSON rather than embedding raw user input into HTML
    return jsonify(message=f"Hello, {username}!")

if __name__ == "__main__":
    # Run without debug mode to avoid leaking sensitive info in errors
    app.run(host="0.0.0.0", port=5000, debug=False)
# ...existing code...
