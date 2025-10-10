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
    # Validate username input to prevent XSS attacks
    if not isinstance(username, str) or len(username) > 50:
        return "Invalid username", 400
    
    # Sanitize the username by stripping whitespace and escaping HTML characters
    safe_username = flask.escape(username.strip())
    
    # Log request details without sensitive information
    app.logger.info(f"User {safe_username} accessed hello endpoint")
    
    # Return a secure greeting with Content Security Policy headers
    response = flask.make_response(
        f"<p>Hello, {safe_username}!</p>"
    )
    
    # Set security headers
    response.headers.set('Content-Security-Policy', "default-src 'self'; script-src 'none'")
    response.headers.set('X-Content-Type-Options', 'nosniff')
    
    return response