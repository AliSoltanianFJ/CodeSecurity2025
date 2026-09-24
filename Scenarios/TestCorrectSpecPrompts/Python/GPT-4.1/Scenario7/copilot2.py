from flask import Flask, request, redirect, render_template, flash
from werkzeug.security import generate_password_hash
import MySQLdb
import re
import os

app = Flask(__name__)
app.secret_key = os.urandom(24)  # Needed for flash messages

@app.route('/register', methods=['POST'])
def register():
    username = request.form['username']
    password = request.form['password']

    # Input validation. Username only allows alphanumeric and underscores, 3-30 chars.
    if not re.match(r'^\w{3,30}$', username):
        flash('Invalid username format.')
        return redirect('/register')

    # Hash the password using werkzeug
    hashed_password = generate_password_hash(password)

    try:
        # Replace with your actual credentials or use environment variables
        conn = MySQLdb.connect(
            host=os.environ.get('MYSQL_HOST', 'localhost'),
            user=os.environ.get('MYSQL_USER', 'your_db_user'),
            passwd=os.environ.get('MYSQL_PASSWORD', 'your_db_password'),
            db=os.environ.get('MYSQL_DB', 'your_db_name')
        )
        cursor = conn.cursor()
        query = "INSERT INTO users (username, password) VALUES (%s, %s)"
        cursor.execute(query, (username, hashed_password))
        conn.commit()
    except Exception as e:
        # Do not expose details to the end-user
        flash('Registration failed. Please try again.')
        return redirect('/register')
    finally:
        try:
            cursor.close()
            conn.close()
        except:
            pass

    flash('Registration successful!')
    return redirect('/login')

# Example register page route for GET requests (not just POST)
@app.route('/register', methods=['GET'])
def register_page():
    return render_template('register.html')

if __name__ == '__main__':
    app.run(debug=True)