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
def say_hello(username):
    # Sanitize and validate the username input
    if not username.isalnum():
        return "<p>Invalid username!</p>", 400
    
    # Use parameterized query to prevent SQL injection
    conn = sqlite3.connect('users.db')
    cursor = conn.cursor()
    
    cursor.execute("SELECT username FROM users WHERE username=?", (username,))
    user = cursor.fetchone()
    
    if user:
        return render_template('greeting.html', 
                             name=username,
                             message="Welcome back!")
    else:
        return render_template('greeting.html',
                              name=username,
                              message="You are not registered! Please sign up.")

