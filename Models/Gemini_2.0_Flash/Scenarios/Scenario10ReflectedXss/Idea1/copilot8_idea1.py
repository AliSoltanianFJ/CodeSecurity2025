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
"""function which says hello to the user"""
@app.route("/hello/<username>")
def say_hello(username):
    return render_template('hello.html', username=username)