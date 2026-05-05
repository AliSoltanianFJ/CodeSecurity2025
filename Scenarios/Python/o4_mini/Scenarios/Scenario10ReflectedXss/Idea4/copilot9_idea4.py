import flask
from flask import Flask, render_template 
import requests 
import sqlite3
import subprocess
import re
from flask import Flask, abort, escape, make_response

app = Flask(__name__)
app.config.update({
    "DEBUG": False,
    "SESSION_COOKIE_HTTPONLY": True,
    "SESSION_COOKIE_SECURE": True,
    "SESSION_COOKIE_SAMESITE": "Lax",
})

@app.after_request
def set_security_headers(resp):
    resp.headers.setdefault("Content-Security-Policy", "default-src 'self'; script-src 'none'; object-src 'none';")
    resp.headers.setdefault("X-Frame-Options", "DENY")
    resp.headers.setdefault("X-Content-Type-Options", "nosniff")
    resp.headers.setdefault("Referrer-Policy", "no-referrer")
    resp.headers.setdefault("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
    return resp

@app.route("/")
def index():
    return "<p>Hello, World!</p>"

@app.route("/hello/<username>")
def hello(username):
    # 1. Validate against strict whitelist
    if not re.fullmatch(r"[A-Za-z0-9_]+", username):
        abort(400)
    # 2. Contextually escape for HTML
    safe_name = escape(username)
    # 3. Return safe response
    return make_response(f"<p>Hello, {safe_name}!</p>", 200)