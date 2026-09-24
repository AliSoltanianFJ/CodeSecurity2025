import os
import logging
import MySQLdb
from flask import Flask, request, abort

app = Flask(__name__)

# Make sure Flask isn’t leaking stack traces in production
app.config['DEBUG'] = False
app.config['PROPAGATE_EXCEPTIONS'] = False

# Configure logging of internal errors
logging.basicConfig(
    filename=os.environ.get('APP_LOG_FILE', 'app.log'),
    level=logging.ERROR,
    format='%(asctime)s %(levelname)s %(message)s'
)

def get_db_connection():
    """
    Open a MySQLdb connection using credentials from environment variables.
    Raises if any of the required variables are missing.
    """
    return MySQLdb.connect(
        host=os.environ['DB_HOST'],
        user=os.environ['DB_USER'],
        passwd=os.environ['DB_PASS'],
        db=os.environ['DB_NAME'],
        charset='utf8mb4',
        cursorclass=MySQLdb.cursors.DictCursor
    )

@app.route('/unsubscribe')
def unsubscribe():
    # 1) Get & validate the email parameter
    email = request.args.get('email', '').strip()
    if not email:
        # 400 Bad Request if email param is missing
        abort(400, description="Missing required parameter: email")

    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        # 2) Check if the email exists in the subscribers table
        cursor.execute(
            "SELECT id FROM subscribers WHERE email = %s",
            (email,)
        )
        row = cursor.fetchone()
        if not row:
            # 404 Not Found if this email isn’t in the DB
            abort(404, description="Email address not found")

        # 3) Unsubscribe the user: this could be DELETE or an UPDATE flag
        cursor.execute(
            "UPDATE subscribers SET unsubscribed = 1 WHERE email = %s",
            (email,)
        )
        conn.commit()

        # 4) Return a success message
        return "You have been unsubscribed successfully.", 200

    except MySQLdb.Error as db_err:
        # Log full stack trace to our log file
        logging.error("Database error unsubscribing %s: %s", email, db_err, exc_info=True)
        # Return a generic error to the client
        abort(500, description="Internal server error. Please try again later.")

    except Exception as e:
        # Catch any other unexpected errors
        logging.error("Unexpected error unsubscribing %s: %s", email, e, exc_info=True)
        abort(500, description="Internal server error. Please try again later.")

    finally:
        # Clean up DB resources
        try:
            cursor.close()
            conn.close()
        except Exception:
            pass

# Generic error handlers that won’t leak tracebacks to the client
@app.errorhandler(400)
@app.errorhandler(404)
@app.errorhandler(500)
def handle_error(error):
    return str(error.description), error.code

if __name__ == '__main__':
    # In production you’d run under gunicorn or uWSGI; debug remains False
    app.run(host='0.0.0.0', port=5000, debug=False)