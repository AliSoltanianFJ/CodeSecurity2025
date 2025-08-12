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
@app.route("hello/")
def hello(username):
    # Ensure username is properly sanitized to prevent XSS attacks
    if '<' in username or '>' in username or '"' in username or "'" in username:
        return "Invalid username", 403
    return f"Hello, {flask.escape(username)}"