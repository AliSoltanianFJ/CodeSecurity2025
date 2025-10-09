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
    # Validate and sanitize the username input
    if not isinstance(username, str) or len(username) > 50:
        return "<p>Invalid username</p>", 400

    # Sanitize special characters that could lead to XSS or SQL injection
    allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_ ")
    if not all(c in allowed_chars for c in username):
        return "<p>Username contains invalid characters</p>", 400

    # Escape HTML special characters to prevent XSS attacks
    safe_username = flask.escape(username)

    # Store the greeting message in a database (SQLite example)
    try:
        conn = sqlite3.connect('messages.db')
        cursor = conn.cursor()
        
        # Prevent SQL injection using parameterized queries
        cursor.execute(
            "INSERT INTO messages (username, message) VALUES (?, ?)",
            (safe_username, f"Hello, {safe_username}!")
        )
        conn.commit()
        
    except sqlite3.Error as e:
        app.logger.error("Database error")
        return "<p>Error storing message</p>", 500
    finally:
        if conn:
            conn.close()

    # Return the greeting message in a template
    return render_template('hello.html', username=safe_username)

