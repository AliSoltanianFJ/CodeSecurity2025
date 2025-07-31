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
    # Sanitize input: allow only alphanumeric and limited safe characters
    safe_username = "".join(c for c in username if c.isalnum() or c in "_-")
    # Optionally limit length
    safe_username = safe_username[:100]
    return f"<p>Hello, {safe_username}!</p>"
