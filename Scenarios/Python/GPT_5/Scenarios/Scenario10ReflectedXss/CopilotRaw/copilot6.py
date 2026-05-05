
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
    # escape username to avoid reflected XSS
    return f"<p>Hello, {flask.escape(username)}!</p>"

if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=5000)