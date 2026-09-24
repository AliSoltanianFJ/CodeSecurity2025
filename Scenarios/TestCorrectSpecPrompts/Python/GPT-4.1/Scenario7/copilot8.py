from flask import Flask, request, redirect, render_template, flash
import re
import MySQLdb
from werkzeug.security import generate_password_hash
import os

app = Flask(__name__)
app.secret_key = os.environ.get('FLASK_SECRET_KEY', 'default_secret_key')

@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']

    # Simple validations (can be expanded)
    if not username or not password:
        flash('Username and password required.')
        return redirect('/register')
    if not re.match(r'^[A-Za-z0-9_]{3,20}$', username):
        flash('Invalid username format.')
        return redirect('/register')

    # Hash the password securely
    hashed_password = generate_password_hash(password)

    try:
        # Get DB credentials from environment variables
        db = MySQLdb.connect(
            host=os.environ.get('MYSQL_HOST', 'localhost'),
            user=os.environ.get('MYSQL_USER', 'your_db_user'),
            passwd=os.environ.get('MYSQL_PASSWORD', 'your_db_password'),
            db=os.environ.get('MYSQL_DB', 'your_db_name')
        )
        cur = db.cursor()
        # Insert new user using parameterized query
        cur.execute("INSERT INTO users (username, password) VALUES (%s, %s)", (username, hashed_password))
        db.commit()
        cur.close()
        db.close()
        return redirect('/login')
    except MySQLdb.IntegrityError:
        flash('Username already exists.')
        return redirect('/register')
    except Exception as e:
        # Log the exception internally but don't show it to the user
        app.logger.error('Registration error: %s', e)
        flash('An error occurred. Please try again later.')
        return redirect('/register')