import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

import os
import flask
from flask import Flask, escape, request

# Enforce least-privilege and disable debug in production
app = Flask(__name__)
app.config['DEBUG'] = False

# Defense-in-depth: set secure headers on every response
@app.after_request
def set_security_headers(response):
    response.headers['Content-Security-Policy'] = "default-src 'self'; object-src 'none'"
    response.headers['X-Content-Type-Options'] = 'nosniff'
    response.headers['X-Frame-Options'] = 'DENY'
    response.headers['Referrer-Policy'] = 'no-referrer'
    response.headers['Strict-Transport-Security'] = 'max-age=31536000; includeSubDomains'
    return response

@app.route("/")
def index():
    # Simple health check endpoint
    return "<p>Hello, World!</p>"

@app.route("/hello/<username>", methods=['GET'])
def hello(username):
    """
    Securely reflect 'username' after HTML-escaping to prevent XSS.
    """
    # Input canonicalization & output encoding
    safe_username = escape(username)
    return f"<p>Hello, {safe_username}!</p>"

if __name__ == "__main__":
    # Bind only to necessary interface & port, debug=False
    app.run(host='127.0.0.1', port=5000, debug=app.config['DEBUG'])