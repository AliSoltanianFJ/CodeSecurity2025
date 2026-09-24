from flask import Flask, request, redirect, flash
from werkzeug.security import generate_password_hash
import MySQLdb
import os

app = Flask(__name__)
app.secret_key = os.environ.get('SECRET_KEY', 'YOU_SHOULD_CHANGE_THIS')

@app.route('/register', methods=['POST'])
def register():
    try:
        username = request.form['username']
        password = request.form['password']

        # Input validation
        if not username or not password:
            flash('Username and password are required.')
            return redirect('/register')

        # Hash the password securely
        hashed_password = generate_password_hash(password)

        # Connect to the database (use environment variables or config for credentials)
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME')
        )
        cursor = db.cursor()

        # Insert new user into database (avoid SQL injection with parameterized query)
        cursor.execute(
            "INSERT INTO users (username, password) VALUES (%s, %s)",
            (username, hashed_password)
        )
        db.commit()
        cursor.close()
        db.close()

        return redirect('/login')
    except Exception as e:
        return f"Error: {str(e)}", 500