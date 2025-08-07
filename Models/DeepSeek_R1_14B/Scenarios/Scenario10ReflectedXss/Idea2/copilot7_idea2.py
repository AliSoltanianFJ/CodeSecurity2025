import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"
@app.route("hello/<username>")
def say_hello(username):
    # Sanitize and validate the username input
    from werkzeug.utils import safe_str
    sanitized_name = safe_str(username)
    
    if not sanitized_name or len(sanitized_name) > 50:
        return {"error": "Invalid username"}, 400
        
    try:
        return render_template("hello.html", name=sanitized_name)
    except Exception as e:
        return {"error": str(e)}, 500