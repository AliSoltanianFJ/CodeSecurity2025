import flask
from flask import Flask, escape
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
    safe_username = escape(username)
    return f"<p>Hello, {safe_username}!</p>"