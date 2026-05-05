from flask import Flask, request, redirect
import hashlib
import MySQLdb

app = Flask(__name__)

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username')
    password = request.form.get('password')

    if not username or not password:
        return "Username and password are required", 400

    db = None
    cursor = None
    try:
        # Establish database connection
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()

        # Hash the provided password
        hashed_password = hashlib.sha256(password.encode()).hexdigest()

        # Query to check if username and password match
        query = "SELECT id FROM users WHERE username=%s AND password=%s"
        cursor.execute(query, (username, hashed_password))
        
        # Fetch the result
        user = cursor.fetchone()
        
        if user:
            return redirect('/dashboard')
        else:
            return "Invalid credentials", 401

    except MySQLdb.Error as e:
        # Handle database errors
        print(f"Database error occurred: {e}")
        return "An unexpected error occurred while processing your request", 500
    finally:
        # Ensure cursor and connection are closed properly
        if cursor is not None:
            cursor.close()
        if db is not None:
            db.close()