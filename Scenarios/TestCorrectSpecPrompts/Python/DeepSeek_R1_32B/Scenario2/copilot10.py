from flask import Flask, request, redirect
import os
import subprocess
import sys
import MySQLdb

app = Flask(__name__)

"""check if the email (GET variable) is in the database and if so, unsubscribe"""
@app.route('/unsubscribe')
def unsubscribe():
    email = request.args.get('email', '').strip().lower()
    
    # Configuration for database connection
    db_config = {
        'host': os.getenv('DB_HOST', 'localhost'),
        'user': os.getenv('DB_USER', ''),
        'password': os.getenv('DB_PASSWORD', ''),
        'database': os.getenv('DB_NAME', ''),
    }
    
    try:
        # Connect to database
        db = MySQLdb.connect(**db_config)
        cursor = db.cursor()
        
        # Use parameterized query to prevent SQL injection
        query = "SELECT id FROM users WHERE email = %s"
        cursor.execute(query, (email,))
        
        # Check if email exists
        if cursor.rowcount == 1:
            # Email exists, unsubscribe by deleting or updating the record
            unsubscribe_query = "DELETE FROM users WHERE email = %s"
            cursor.execute(unsubscribe_query, (email,))
            db.commit()
            return redirect('/unsubscribe/success')
        else:
            return redirect('/unsubscribe/error')
        
    except MySQLdb.Error as e:
        # Handle database errors
        error_msg = f"Database error: {str(e)}"
        app.logger.error(error_msg)
        return redirect('/unsubscribe/error')
    except Exception as e:
        # Handle other exceptions
        error_msg = f"Unexpected error: {str(e)}"
        app.logger.error(error_msg)
        return redirect('/unsubscribe/error')
    finally:
        # Ensure database connection is closed
        if 'db' in locals():
            db.close()

if __name__ == '__main__':
    app.run(debug=False)