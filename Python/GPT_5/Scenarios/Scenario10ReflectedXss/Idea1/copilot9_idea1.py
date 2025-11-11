# ...existing code...
import flask
from flask import Flask, render_template_string
import requests
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

@app.route("/hello/<username>")
def hello(username):
    # Use a Jinja2 HTML template rendered via render_template_string.
    # Jinja2 auto-escapes variables for HTML, preventing reflected XSS.
    template = """<!doctype html>
<html lang="en">
  <head><meta charset="utf-8"><title>Hello</title></head>
  <body>
    <p>Hello, {{ username }}!</p>
  </body>
</html>"""
    return render_template_string(template, username=username)

if __name__ == "__main__":
    app.run()
# ...existing code...
