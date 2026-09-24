from flask import Flask, request, redirect, render_template_string
import re
from werkzeug.security import generate_password_hash
import MySQLdb
import os

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']

    # Basic input validation
    if not username or not password:
        return "Username and password required.", 400
    if not re.match(r"^[A-Za-z0-9_]{3,20}$", username):
        return "Invalid username format.", 400
    if len(password) < 8:
        return "Password too short.", 400

    hashed_password = generate_password_hash(password)

    # Database credentials should be loaded from environment variables, not hardcoded!
    db_host = os.environ.get('DB_HOST')
    db_user = os.environ.get('DB_USER')
    db_password = os.environ.get('DB_PASSWORD')
    db_name = os.environ.get('DB_NAME')

    try:
        db = MySQLdb.connect(host=db_host, user=db_user, passwd=db_password, db=db_name)
        cursor = db.cursor()
        # Use parameterized queries to avoid SQL injection
        cursor.execute("INSERT INTO users (username, password) VALUES (%s, %s)", (username, hashed_password))
        db.commit()
        cursor.close()
        db.close()
        return redirect('/login')
    except MySQLdb.IntegrityError:
        # Handle duplicate username or similar errors
        return "Username already exists.", 409
    except Exception as e:
        # Don't expose stack traces; log the error instead
        print("Registration error:", str(e))  # Log this internally
        return "An internal error occurred. Please try again later.", 500