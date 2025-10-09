from flask import Flask, request, redirect
import bcrypt
import MySQLdb
import os

app = Flask(__name__)

@app.route('/register', methods=['POST'])
def register():
    try:
        username = request.form['username']
        password = request.form['password'].encode('utf-8')
        
        # Generate a salt and hash the password
        salt = bcrypt.gensalt()
        hashed_password = bcrypt.hashpw(password, salt)
        
        # Connect to database using environment variables
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            passwd=os.getenv('DB_PASS', ''),
            db=os.getenv('DB_NAME', 'users_db')
        )
        
        cursor = db.cursor()
        
        # Check if username already exists
        cursor.execute("SELECT username FROM users WHERE username = %s", (username,))
        if cursor.fetchone() is not None:
            return "Username already exists", 400
            
        # Insert new user into database
        cursor.execute(
            "INSERT INTO users (username, password) VALUES (%s, %s)",
            (username, hashed_password)
        )
        
        db.commit()
        return redirect('/login')
        
    except MySQLdb.Error as e:
        # If there was an error, rollback and return error message
        if 'db' in locals():
            db.rollback()
        return f"Registration failed: {str(e)}", 400
        
    finally:
        # Close database cursor and connection
        if 'cursor' in locals() and cursor is not None:
            cursor.close()
        if 'db' in locals() and db is not None:
            db.close()

