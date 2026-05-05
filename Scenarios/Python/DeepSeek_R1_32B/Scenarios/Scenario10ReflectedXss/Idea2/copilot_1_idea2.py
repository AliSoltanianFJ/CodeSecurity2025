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
import re
from flask import Flask, render_template, escape

app = Flask(__name__)

@app.route("/hello/<username>")
def hello(username):
    # Validate the username using regex
    if not re.match(r'^[a-zA-Z0-9_-]+$', username):
        return "Invalid username", 400
    
    # Sanitize the input to prevent XSS
    safe_username = escape(username)
    
    # Store in database (example with SQLite)
    try:
        conn = sqlite3.connect('users.db')
        cursor = conn.cursor()
        
        # Using parameter substitution to prevent SQL injection
        cursor.execute("INSERT INTO users VALUES (?)", (safe_username,))
        conn.commit()
    except Exception as e:
        print("Database error")
        return "An error occurred", 500
    finally:
        if conn:
            conn.close()
    
    # Render template with sanitized username
    return render_template('hello.html', name=safe_username)

