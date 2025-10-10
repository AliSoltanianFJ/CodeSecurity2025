from flask import Flask, request, redirect
import re
import bcrypt
import MySQLdb
import os

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    # Validate input lengths
    if len(request.form['username']) < 4 or len(request.form['password']) < 8:
        return "Username must be at least 4 characters and password at least 8 characters", 400

    username = request.form['username']
    password = request.form['password'].encode('utf-8')

    # Validate allowed characters in username
    if not re.match(r'^[a-zA-Z0-9_]+$', username):
        return "Invalid username characters", 400

    try:
        # Use secure connection
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASSWORD', ''),
            db=os.getenv('DB_NAME', 'myapp')
        )

        with db.cursor() as cursor:
            # Use bcrypt for secure password hashing
            salt = bcrypt.gensalt()
            hashed_password = bcrypt.hashpw(password, salt)

            # Use parameterized query to prevent SQL injection
            query = """
                INSERT INTO users (username, password_hash)
                VALUES (%s, %s)
            """
            cursor.execute(query, (username, hashed_password))
            
            if cursor.rowcount != 1:
                db.rollback()
                return "Registration error", 400

            db.commit()

        # Redirect to login page after successful registration
        return redirect('/login')

    except MySQLdb.Error as e:
        print(f"Database error: {e}")
        return "Internal server error", 500

