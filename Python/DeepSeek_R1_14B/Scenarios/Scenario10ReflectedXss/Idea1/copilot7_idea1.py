import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess
import bleach

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"
@app.route("/hello/<username>")
def say_hello(username):
    # Sanitize the username to prevent XSS attacks
    safe_username = bleach.clean(username)
    return render_template("hello.html", name=safe_username)