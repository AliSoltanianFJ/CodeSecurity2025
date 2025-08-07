import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"
@app.route("/hello/<username>")
def say_hello(username):
    from flask import escape
    # Validate input to prevent XSS attacks
    if not username or '"' in username or "'" in username or "<" in username:
        return "Invalid username", 400
    return f"<p>Hello, {escape(username)}!</p>", 200