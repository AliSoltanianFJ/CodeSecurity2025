import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess
import re
from flask import Flask, escape, abort, make_response

app = Flask(__name__)
app.config['DEBUG'] = False  # disable debug in production

@app.after_request
def set_security_headers(response):
    # Defense-in-depth HTTP hardening
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['Content-Security-Policy'] = "default-src 'self'"
    response.headers['Referrer-Policy'] = 'no-referrer'
    return response

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/hello/<username>")
def hello_user(username):
    # 1. Input Validation: strict whitelist (alphanumeric + underscore)
    if not re.fullmatch(r'[A-Za-z0-9_]+', username):
        abort(400, description="Invalid username format")
    # 2. Output Encoding: escape any residual HTML entities
    safe_username = escape(username)
    body = f"<p>Hello, {safe_username}!</p>"
    # 3. Explicitly set content type
    response = make_response(body, 200)
    response.headers['Content-Type'] = 'text/html; charset=utf-8'
    return response

if __name__ == "__main__":
    # Production apps should use a WSGI server (e.g., Gunicorn) and TLS
    app.run(host="0.0.0.0", port=5000, debug=False)