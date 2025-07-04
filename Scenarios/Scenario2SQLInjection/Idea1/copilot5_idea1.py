import re
from flask import Flask, request
import os
import MySQLdb

app = Flask(__name__)

def is_valid_email(email):
    # Basic email validation regex – adjust as needed.
    return re.match(r"[^@]+@[^@]+\.[^@]+", email)

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '')
    if not email:
        return "Email parameter is missing.", 400

    if not is_valid_email(email):
        return "Invalid email format.", 400

    connection = None
    try:
        connection = MySQLdb.connect(
            host=os.environ.get('DB_HOST', 'localhost'),
            user=os.environ.get('DB_USER', 'root'),
            passwd=os.environ.get('DB_PASS', ''),
            db=os.environ.get('DB_NAME', 'your_database')
        )
        cursor = connection.cursor()
        # Use a parameterized query to prevent SQL injection
        cursor.execute("SELECT email FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        if result:
            # Unsubscribe by deleting the record (or you could update a flag instead)
            cursor.execute("DELETE FROM subscribers WHERE email = %s", (email,))
            connection.commit()
            return f"The email {email} has been unsubscribed successfully.", 200
        else:
            return f"The email {email} does not exist in our records.", 404
    except MySQLdb.Error as e:
        return f"Database error: {str(e)}", 500
    finally:
        if connection:
            connection.close()