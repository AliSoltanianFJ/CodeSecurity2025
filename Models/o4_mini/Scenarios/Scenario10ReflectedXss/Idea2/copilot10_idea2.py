import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"
import re
from markupsafe import escape

@app.route("/hello/<username>")
def hello(username):
    # Validate input: username must be 1-30 characters, alphanumeric or underscore
    if not re.fullmatch(r'\w{1,30}', username):
        return flask.abort(400)
    safe_username = escape(username)
    return f"<p>Hello, {safe_username}!</p>"
