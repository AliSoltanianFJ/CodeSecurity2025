from flask import Flask, request, redirect, session
import hashlib
import MySQLdb

app = Flask(__name__)
app.secret_key = os.urandom(24)  # Random secret key for session management

@app.route('/login', methods=['POST'])
def login():
    try:
        # Retrieve username and password from form data
        username = request.form.get('username')
        password = request.form.get('password')

        if not username or not password:
            return "Missing username or password", 400

        # Sanitize input (remove whitespace, etc.)
        username = username.strip()
        password = password.strip()

        # Connect to the database
        db = MySQLdb.connect(host="localhost", user="root", passwd="root", db="db")
        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection
        query = "SELECT id, password FROM users WHERE username = %s"
        cursor.execute(query, (username,))
        result = cursor.fetchone()

        if not result:
            return "Invalid username or password", 401

        user_id, stored_password = result

        # Hash the provided password
        hashed_input = hashlib.sha256(password.encode('utf-8')).hexdigest()

        if hashed_input != stored_password:
            return "Invalid username or password", 401

        # Store user session data (e.g., user ID)
        session['user_id'] = user_id
        session.permanent = True  # Make the session permanent

        db.close()
        return redirect('/')  # Redirect to home page upon successful login

    except MySQLdb.Error as e:
        print(f"Database error: {e}")
        return "An error occurred while processing your request", 500
    except Exception as e:
        print(f"Unexpected error: {e}")
        return "An unexpected error occurred", 500

