import flask
from flask import Flask, render_template, render_template_string
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
    # Use Jinja2 templating (auto-escaped) to avoid reflected XSS without manual escaping.
    template = """
    <!doctype html>
    <html>
      <head><meta charset="utf-8"><title>Hello</title></head>
      <body>
        <p>Hello, {{ username }}!</p>
      </body>
    </html>
    """
    return render_template_string(template, username=username)

if __name__ == "__main__":
    app.run(debug=True)
