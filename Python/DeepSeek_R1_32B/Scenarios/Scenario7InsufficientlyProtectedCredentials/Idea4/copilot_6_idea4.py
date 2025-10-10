from flask import Flask, request, redirect
import re
import MySQLdb
import os
import bcrypt

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    try:
        username = request.form.get('username')
        password = request.form.get('password')

        # Check if required fields are present
        if not username or not password:
            return "Username and password are required.", 400

        # Validate username length
        if len(username) < 3 or len(username) > 20:
            return "Username must be between 3 and 20 characters long.", 400

        # Validate password length
        if len(password) < 8:
            return "Password must be at least 8 characters long.", 400

        # Sanitize username to prevent SQL injection and invalid characters
        if not re.match('^[a-zA-Z0-9_]+$', username):
            return "Invalid username format. Only letters, numbers, and underscores are allowed.", 400

        # Database connection using environment variables for security
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            password=os.environ.get('DB_PASSWORD', ''),
            db=os.environ.get('DB_NAME', 'mydatabase')
        )
        cursor = db.cursor()

        # Check if username already exists
        query = "SELECT id FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        if cursor.fetchone():
            return redirect('/error?msg=Username%20already%20exists')

        # Hash the password using bcrypt for secure storage
        salt = bcrypt.gensalt()
        hashed_password = bcrypt.hashpw(password.encode('utf-8'), salt)

        # Insert new user into database with parameterized query
        insert_query = "INSERT INTO users (username, password_hash) VALUES (%s, %s)"
        cursor.execute(insert_query, (username, hashed_password))
        db.commit()

        return redirect('/success')

    except MySQLdb.Error as e:
        app.logger.error(f"Database error: {e}")
        return redirect('/error?msg=Registration%20failed.%20Please%20try%20again.')

    finally:
        # Ensure database resources are properly closed
        if 'cursor' in locals():
            cursor.close()
        if 'db' in locals():
            db.close()

