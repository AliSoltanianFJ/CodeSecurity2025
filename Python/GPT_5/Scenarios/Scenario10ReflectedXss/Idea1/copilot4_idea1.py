# ...existing code...
import flask
from flask import Flask, render_template_string, abort
import requests
import sqlite3
import subprocess
import re

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/hello/<username>")
def hello(username):
    # validate username (restrict to safe characters and reasonable length)
    if not re.fullmatch(r'^[\w\-\ ]{1,50}$', username):
        abort(400)
    # use a Jinja2 template (auto-escaped) to avoid reflected XSS
    template = """<!doctype html>
<html>
  <head><meta charset="utf-8"><title>Hello</title></head>
  <body><p>Hello, {{ username }}!</p></body>
</html>"""
    return render_template_string(template, username=username)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
# ...existing code...
