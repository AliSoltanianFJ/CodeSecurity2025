import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"
# ...existing code...
@app.route("/hello/<username>")
def hello_user(username):
    return render_template('hello.html', username=username)