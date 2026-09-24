from flask import Flask, request, redirect, render_template, flash
from werkzeug.security import generate_password_hash
import MySQLdb
import os

app = Flask(__name__)
app.secret_key = os.urandom(24)  # To enable flash messages and session

def get_db_connection():
    # DON'T hardcode credentials! Use environment variables
    return MySQLdb.connect(
        host=os.environ.get('MYSQL_HOST', 'localhost'),
        user=os.environ.get('MYSQL_USER'),
        passwd=os.environ.get('MYSQL_PASSWORD'),
        db=os.environ.get('MYSQL_DB')
    )

@app.route('/register', methods=['POST'])
def register():
    username = request.form.get('username', '').strip()
    password = request.form.get('password', '')

    if not username or not password:
        flash("Username and password are required.")
        return redirect('/register')

    import re
    # Basic validation for username (example, can be made stricter)
    if not re.match(r'^\w{3,30}$', username):
        flash("Invalid username format.")
        return redirect('/register')

    try:
        db = get_db_connection()
        cur = db.cursor()

        # Check if username already exists
        cur.execute("SELECT id FROM users WHERE username = %s", (username,))
        if cur.fetchone():
            flash("Username already exists.")
            return redirect('/register')

        # Securely hash the password
        hashed_password = generate_password_hash(password)

        # Insert the user into the database
        cur.execute("INSERT INTO users (username, password) VALUES (%s, %s)", (username, hashed_password))
        db.commit()
        flash("Registration successful! Please log in.")
        return redirect('/login')
    except Exception:
        # Do not expose the stack trace
        flash("An internal error occurred. Please try again later.")
        return redirect('/register')
    finally:
        try:
            cur.close()
            db.close()
        except:
            pass  # In case db or cur is not defined

# You may also need the routes for /register (GET) and /login