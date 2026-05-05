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
    # Validate input: only allow alphanumeric characters and underscores
    if not username.isalnum():
        return flask.abort(400, "Invalid username")
    
    # Use Jinja2 template for output to prevent XSS attacks
    return render_template("greeting.html", username=username)

