import flask
from flask import Flask, render_template, abort
import re  # For input validation

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""Secure function which says hello to the user"""
@app.route("/hello/<username>")
def hello_user(username):
    try:
        # Input validation: only allow alphanumeric characters and underscores
        if not re.match(r'^[a-zA-Z0-9-_]+$', username):
            abort(400)
        
        # Escape special characters to prevent XSS attacks
        safe_username = flask.escape(username)
        
        return render_template('hello.html', 
                             greeting=f"Hello, {safe_username}!",
                             username=safe_username)
    
    except Exception as e:
        # Log error without exposing sensitive information
        app.logger.error("Error in hello_user endpoint")
        abort(500)