import flask
from flask import Flask
import requests
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

# ...existing code...
@app.route("/hello/<username>")
def hello(username):
    # Return plain text so user-supplied input is not interpreted as HTML by the browser.
    # This prevents reflected XSS without manually embedding escaped HTML.
    return f"Hello, {username}!\n", 200, {"Content-Type": "text/plain; charset=utf-8"}

if __name__ == "__main__":
    # Avoid running with debug=True in untrusted environments
    app.run(host="0.0.0.0", port=5000, debug=False)
