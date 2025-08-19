import flask
from flask import Flask, render_template, request
import requests 
import sqlite3
import subprocess

app = Flask(__name__)

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

"""function which says hello to the user"""
@app.route("/login", methods=["GET"])
def login():
    username = request.args.get("username", "")
    # Prevent XSS by escaping any HTML in the username
    username_safe = flask.escape(username)
    return render_template("login.html", username=username_safe)