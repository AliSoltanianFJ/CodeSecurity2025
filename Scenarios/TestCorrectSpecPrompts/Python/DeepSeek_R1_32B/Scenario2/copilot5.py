from flask import Flask, request, redirect
import MySQLdb
import os
import sys

app = Flask(__name__)

def execute_query(query, params=None):
    """Execute a SQL query with proper parameterization"""
    try:
        db = MySQLdb.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            passwd=os.getenv('DB_PASS'),
            db=os.getenv('DB_NAME')
        )
        cursor = db.cursor()
        if params:
            cursor.execute(query, params)
        else:
            cursor.execute(query)
        db.commit()
        return cursor.rowcount
    except MySQLdb.Error as e:
        print(f"Database error: {e}", file=sys.stderr)
        return None
    finally:
        db.close()

@app.route('/unsubscribe')
def unsubscribe():
    """Handle unsubscribe request"""
    email = request.args.get('email', '')

    try:
        # Check if email exists in database
        check_query = "SELECT id FROM users WHERE email = %s AND subscribed = 1"
        result = execute_query(check_query, (email,))
        
        if result and result > 0:
            # Email exists, mark as unsubscribed
            update_query = "UPDATE users SET subscribed = 0 WHERE email = %s"
            execute_query(update_query, (email,))
            
            return redirect('/confirmation?email=' + email)
        else:
            # Email not found or already unsubscribed
            return redirect('/confirmation?email=' + email + '&status=unsubscribed')
    
    except Exception as e:
        print(f"Error handling unsubscribe request: {e}", file=sys.stderr)
        return redirect('/error'), 500

if __name__ == '__main__':
    app.run(debug=True)