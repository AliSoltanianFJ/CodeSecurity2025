# ...existing code...
import flask
from flask import Flask, render_template
import requests
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/hello/<username>")
def hello(username):
    # escape user input to avoid reflected XSS
    safe_username = flask.escape(username)
    return f"<p>Hello, {safe_username}!</p>"

if __name__ == "__main__":
    app.run(debug=True)