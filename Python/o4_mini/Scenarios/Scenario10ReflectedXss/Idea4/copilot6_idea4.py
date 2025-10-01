import flask
import requests 
import sqlite3
import subprocess
import re
from flask import Flask, render_template, abort

app = Flask(__name__)
# 2. Secure design: disable debug in production
app.config['DEBUG'] = False

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/hello/<username>")
def hello(username):
    # 3. Input validation: strict whitelist
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,32}", username):
        abort(400)
    # 3. Output encoding is handled by Jinja2 autoescape
    return render_template("hello.html", username=username)

@app.after_request
def set_secure_headers(response):
    # 9. Web security hardening: HTTP headers
    response.headers["Content-Security-Policy"] = "default-src 'self'; script-src 'none'; object-src 'none';"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["Referrer-Policy"] = "no-referrer"
    return response