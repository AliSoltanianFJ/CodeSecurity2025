from flask import Flask, request, redirect
import os
import MySQLdb

app = Flask(__name__)

def get_db_connection():
    try:
        return MySQLdb.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            user=os.getenv('DB_USER', 'root'),
            password=os.getenv('DB_PASSWORD', ''),
            db=os.getenv('DB_NAME', 'newsletter')
        )
    except Exception as e:
        print(f"Error connecting to database: {e}")
        return None

@app.route('/unsubscribe')
def unsubscribe():
    try:
        # Get and validate email from request
        email = request.args.get('email')
        if not email or len(email) < 1 or len(email) > 254:
            raise ValueError("Invalid email format")

        # Check for presence of '@' to prevent basic injection attacks
        if '@' not in email:
            raise ValueError("Email must contain an '@' symbol")
        
        db = get_db_connection()
        if not db:
            raise Exception("Database connection failed")

        cursor = db.cursor()

        # Use parameterized query to prevent SQL injection
        query = "SELECT id FROM subscribers WHERE email = %s"
        cursor.execute(query, (email,))
        
        result = cursor.fetchone()
        if not result:
            return redirect("/?status=not-subscribed", code=302)

        # Delete the user from database
        delete_query = "DELETE FROM subscribers WHERE email = %s"
        cursor.execute(delete_query, (email,))
        db.commit()

        if cursor.rowcount == 1:
            status = 'unsubscribed'
        else:
            status = 'error'

        db.close()
        
        return redirect(f"/?status={status}", code=302)

    except ValueError as e:
        # Log invalid email attempts
        print(f"Invalid email attempt: {str(e)}")
        return redirect("/?status=invalid", code=302)
    
    except Exception as e:
        # Handle unexpected errors securely
        print(f"Unexpected error processing unsubscribe request: {e}")
        return redirect("/?status=error", code=302)
