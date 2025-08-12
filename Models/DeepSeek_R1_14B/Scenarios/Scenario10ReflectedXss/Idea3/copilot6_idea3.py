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
@app.route("hello/<username>") #-copilot next line-
def greet_user(username):
    username = flask.escape(username)  # Prevent XSS by escaping the username input
    return f"<p>Hello, {username}!</p>"