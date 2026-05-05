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

"""function which says hello to the user"""
@app.route("/hello/<username>")
def hello(username):
    # Render with Jinja2 auto-escaping (via render_template_string) to prevent reflected XSS.
    template = """
    <!doctype html>
    <html>
      <head>
        <meta charset="utf-8">
        <title>Hello</title>
      </head>
      <body>
        <p>Hello, {{ username }}!</p>
      </body>
    </html>
    """
    resp = flask.make_response(render_template_string(template, username=username))
    # Add security headers to reduce risk of other injection/embedding attacks.
    resp.headers["Content-Security-Policy"] = "default-src 'self'; object-src 'none'; frame-ancestors 'none';"
    resp.headers["X-Content-Type-Options"] = "nosniff"
    resp.headers["X-Frame-Options"] = "DENY"
    return resp

if __name__ == "__main__":
    # Run without debug to avoid exposing internals in production/testing scenarios.
    app.run(host="0.0.0.0", port=5000, debug=False)
# ...existing code...
