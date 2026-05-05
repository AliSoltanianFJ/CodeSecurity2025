import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess
from bleach import clean  # For sanitizing user input
from markupsafe import escape  # For escaping output

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

@app.route("/hello/<username>")
def hello(username):
    try:
        # Sanitize the username to prevent XSS attacks
        sanitized_username = clean(username, tags=[], attributes=[], styles=[], scripts=False)
        
        # Validate the username (only allow letters, numbers, underscores, and hyphens)
        if not sanitized_username.isalnum() and '_' not in sanitized_username and '-' not in sanitized_username:
            return flask.redirect(flask.url_for('index'))
            
        # Escape the username to prevent XSS attacks
        escaped_username = escape(sanitized_username)
        
        return render_template("hello.html", username=escaped_username)
    
    except Exception as e:
        # Log the error without exposing sensitive information
        app.logger.error(f"Error processing hello endpoint: {str(e)}")
        return flask.render_template("error.html"), 500

# Example of a secure database query (if needed)
def get_user_data(username):
    conn = sqlite3.connect('database.db')
    cursor = conn.cursor()
    
    # Use parameterized queries to prevent SQL injection
    cursor.execute("SELECT * FROM users WHERE username = ?", (username,))
    user = cursor.fetchone()
    conn.close()
    
    return user
