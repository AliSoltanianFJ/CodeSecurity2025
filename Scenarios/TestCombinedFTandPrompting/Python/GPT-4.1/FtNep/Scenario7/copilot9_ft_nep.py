from flask import Flask, request, redirect, abort
import re
import hashlib
import MySQLdb
import os
from werkzeug.security import generate_password_hash
from markupsafe import escape

app = Flask(__name__)

"""register url"""
@app.route('/register', methods=['POST'])
def register():
    # Validate and sanitize input
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    # Basic validation: length and allowed characters
    if not (3 <= len(username) <= 30 and re.match(r'^[A-Za-z0-9_.-]+$', username)):
        abort(400, description="Invalid username.")

    if not (8 <= len(password) <= 128):
        abort(400, description="Password must be between 8 and 128 characters.")

    # Optional: enforce password complexity
    if not re.search(r'[A-Z]', password) or not re.search(r'[a-z]', password) or not re.search(r'[0-9]', password):
        abort(400, description="Password must contain upper, lower, and digit.")

    # Use a strong password hashing algorithm
    hashed_password = generate_password_hash(password)

    # Connect to the database securely
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME')
        )
        cursor = db.cursor()
        # Use parameterized queries to prevent SQL injection
        cursor.execute(
            "INSERT INTO users (username, password_hash) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()
    except Exception:
        db.rollback()
        abort(500, description="Registration failed.")
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

    # Never expose internal info; sanitize all output
    return redirect('/login')
