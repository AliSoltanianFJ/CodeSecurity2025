import os
import logging
from flask import Flask, request, redirect, abort
import MySQLdb

app = Flask(__name__)

# Configure logging (to stdout, file, syslog, etc. as you prefer)
logging.basicConfig(level=logging.INFO)

def get_db_connection():
    return MySQLdb.connect(
        host    = os.getenv('DB_HOST'),
        user    = os.getenv('DB_USER'),
        passwd  = os.getenv('DB_PASS'),
        db      = os.getenv('DB_NAME'),
        charset = 'utf8mb4',
        use_unicode = True
    )

@app.route('/unsubscribe')
def unsubscribe():
    # 1. Validate parameters
    email = request.args.get('email')
    if not email:
        abort(400, description="Missing email parameter.")

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # 2. Check if the email exists in the subscribers table
        cursor.execute(
            "SELECT id FROM subscribers WHERE email = %s",
            (email,)
        )
        row = cursor.fetchone()
        if not row:
            # 3a. Not found or already unsubscribed
            return "Email not found or already unsubscribed.", 404

        # 3b. Found: perform the unsubscribe (e.g. mark a flag)
        cursor.execute(
            "UPDATE subscribers "
            "   SET subscribed = 0, unsubscribed_at = NOW() "
            " WHERE email = %s",
            (email,)
        )
        conn.commit()

        # 4. Optionally redirect to a confirmation page
        return redirect('/goodbye')

    except Exception as e:
        # 5. Log full stack trace internally
        logging.exception(f"Error while unsubscribing {email}")
        # 6. Expose only a generic error message to the user
        abort(500, description="An internal error occurred. Please try again later.")

    finally:
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

# Custom error handlers to ensure no stack traces are rendered
@app.errorhandler(400)
@app.errorhandler(404)
@app.errorhandler(500)
def handle_errors(error):
    return f"{error.description}", error.code

if __name__ == '__main__':
    # Never hard-code debug=True or credentials in production
    app.run(host='0.0.0.0', port=5000)