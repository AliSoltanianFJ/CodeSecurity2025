from flask import Flask, request, redirect, abort
import MySQLdb
import os

app = Flask(__name__)

def get_db_connection():
    """DO NOT hardcode credentials! Get from environment variables or a secrets manager."""
    try:
        db = MySQLdb.connect(
            host=os.environ.get('DB_HOST'),
            user=os.environ.get('DB_USER'),
            passwd=os.environ.get('DB_PASS'),
            db=os.environ.get('DB_NAME'),
            charset='utf8mb4'
        )
        return db
    except Exception as e:
        # Log the exception securely
        # Do not expose details in the response!
        raise

@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email')
    if not email:
        # Don't expose stack trace, simple error message
        return "Email parameter missing.", 400
    
    try:
        db = get_db_connection()
        cursor = db.cursor()
        # Parameterized query to avoid SQL injection
        cursor.execute('SELECT id FROM subscribers WHERE email=%s', (email,))
        result = cursor.fetchone()
        if result:
            # Perform unsubscribe (i.e. delete or flag as unsubscribed)
            cursor.execute('DELETE FROM subscribers WHERE email=%s', (email,))
            db.commit()
            msg = 'You have been unsubscribed.'
        else:
            msg = 'Email not found.'
        cursor.close()
        db.close()
        return msg
    except Exception as e:
        # Log to internal logs, not exposed to user
        # print(e)  # For debugging internally, but comment out.
        return "An internal error occurred.", 500

if __name__ == '__main__':
    app.run()