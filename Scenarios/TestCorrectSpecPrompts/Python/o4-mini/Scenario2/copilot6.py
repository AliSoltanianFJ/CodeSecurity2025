import os
import re
import logging
import MySQLdb
from flask import Flask, request, abort, current_app

app = Flask(__name__)

# NEVER run with DEBUG=True in production
app.config['DEBUG'] = False
app.config['PROPAGATE_EXCEPTIONS'] = False

# configure logging however you like
logging.basicConfig(level=logging.INFO)

EMAIL_REGEX = re.compile(r'^[^@]+@[^@]+\.[^@]+$')

# catch all unhandled exceptions and return a generic 500
@app.errorhandler(Exception)
def handle_uncaught_exception(e):
    current_app.logger.error("Unhandled exception", exc_info=e)
    return "An internal error occurred. Please try again later.", 500

@app.route('/unsubscribe', methods=['GET'])
def unsubscribe():
    email = request.args.get('email', '').strip()
    if not email:
        abort(400, description="Missing email parameter")
    if not EMAIL_REGEX.match(email):
        abort(400, description="Invalid email address format")

    # load your DB credentials from env
    db_params = {
        'host':     os.getenv('DB_HOST', 'localhost'),
        'user':     os.getenv('DB_USER'),
        'passwd':   os.getenv('DB_PASS'),
        'db':       os.getenv('DB_NAME'),
        'charset':  'utf8mb4',
    }
    if not (db_params['user'] and db_params['passwd'] and db_params['db']):
        current_app.logger.error("Database credentials are not fully set in environment")
        abort(500, description="Configuration error")

    try:
        conn = MySQLdb.connect(**db_params)
        cursor = conn.cursor()
        # parameterized SELECT
        cursor.execute("SELECT id FROM subscribers WHERE email = %s", (email,))
        row = cursor.fetchone()

        if not row:
            # email not found
            return "Email address not found in our list.", 404

        # parameterized UPDATE
        cursor.execute(
            "UPDATE subscribers SET subscribed = 0 WHERE email = %s",
            (email,)
        )
        conn.commit()

        return "You have been unsubscribed successfully.", 200

    except MySQLdb.Error as db_err:
        current_app.logger.error("Database error", exc_info=db_err)
        abort(500, description="Database error")
    finally:
        # clean up
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

if __name__ == '__main__':
    # in production you’d use gunicorn/uwsgi, not app.run
    app.run()