import os
import re
import logging
from flask import Flask, request, redirect, abort
import MySQLdb

app = Flask(__name__)

# Ensure we never run in debug mode in production
app.config['DEBUG'] = False

# Simple email regex; you may wish to use a more robust validator
EMAIL_REGEX = re.compile(r"^[^@]+@[^@]+\.[^@]+$")

def is_valid_email(email: str) -> bool:
    return bool(EMAIL_REGEX.match(email))

def get_db_connection():
    """
    Create a new DB connection using credentials from environment variables.
    Expects: DB_HOST, DB_USER, DB_PASS, DB_NAME
    """
    try:
        return MySQLdb.connect(
            host=os.environ['DB_HOST'],
            user=os.environ['DB_USER'],
            passwd=os.environ['DB_PASS'],
            db=os.environ['DB_NAME'],
            charset='utf8mb4',
            cursorclass=MySQLdb.cursors.DictCursor
        )
    except KeyError as e:
        # Missing environment variable
        app.logger.critical("Missing DB credential env var: %s", e)
        raise
    except MySQLdb.Error as e:
        app.logger.error("Could not connect to DB: %s", e)
        raise

@app.route('/unsubscribe')
def unsubscribe():
    # 1) Get & validate the email parameter
    email = request.args.get('email', '').strip()
    if not email:
        abort(400, description="Missing email parameter.")
    if not is_valid_email(email):
        abort(400, description="Invalid email format.")

    # 2) Connect to the database
    try:
        db = get_db_connection()
        cursor = db.cursor()
    except Exception:
        # We’ve already logged the detailed error
        return "Internal server error. Please try again later.", 500

    try:
        # 3) Check if the email exists
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        result = cursor.fetchone()
        if not result:
            # 404 if not in our subscription list
            abort(404, description="Email not found in subscription list.")

        # 4) Unsubscribe
        cursor.execute(
            "UPDATE subscribers SET subscribed = FALSE WHERE email = %s",
            (email,)
        )
        db.commit()

    except MySQLdb.Error as e:
        # Log full error server-side, but don’t send back stack trace
        app.logger.error("DB error on unsubscribe(%s): %s", email, e)
        return "Internal server error. Please try again later.", 500

    finally:
        cursor.close()
        db.close()

    # 5) Redirect or show a success message
    return redirect('/unsubscribe-success')

@app.route('/unsubscribe-success')
def unsubscribe_success():
    return "You have been successfully unsubscribed.", 200

if __name__ == '__main__':
    # Do not enable debug on a production machine
    app.run(host='0.0.0.0', port=int(os.environ.get('PORT', 5000)), debug=False)