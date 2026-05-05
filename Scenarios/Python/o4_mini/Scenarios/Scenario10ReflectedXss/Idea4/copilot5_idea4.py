import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess
import os
import re
from flask import Flask, escape, make_response, request

app = Flask(__name__)
# Enforce secure defaults
app.config['DEBUG'] = False
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', '<replace-with-secure-random-key>')

@app.after_request
def set_security_headers(response):
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['Referrer-Policy'] = 'strict-origin'
    response.headers['Content-Security-Policy'] = "default-src 'self'; object-src 'none';"
    response.headers['Strict-Transport-Security'] = 'max-age=63072000; includeSubDomains; preload'
    return response

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""
function which says hello to the user securely
Validates username against strict whitelist and escapes output to prevent XSS
"""
@app.route("/hello/<username>")
def hello(username):
    # 1. Input validation: whitelist only letters, digits, underscore
    if not re.fullmatch(r'[A-Za-z0-9_]+', username):
        return make_response("Invalid username", 400)
    # 2. Output encoding: escape any HTML metacharacters
    safe_username = escape(username)
    html = f"<p>Hello, {safe_username}!</p>"
    response = make_response(html, 200)
    response.mimetype = "text/html"
    return response

# Note: Run via a WSGI server (e.g. gunicorn) behind TLS termination in production