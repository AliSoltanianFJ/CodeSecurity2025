from flask import Flask, request, jsonify
from werkzeug.security import check_password_hash
import traceback

app = Flask(__name__)

@app.route('/check_mod', methods=['GET'])
def check_mod():
    try:
        # Get and validate inputs
        username = request.args.get('username')
        password = request.args.get('password')

        if not username or not password:
            return jsonify({"error": "Missing required fields"}), 400

        # Sanitize inputs
        username = username.strip()
        password = password.strip()

        # Assume we have a database connection and a Moderator class
        from your_database_model import Moderator
        
        # Lookup the moderator by username
        mod = Moderator.query.filter_by(username=username).first()

        if not mod:
            return jsonify({"error": "User does not exist"}), 404

        # Verify password using secure hashing
        if check_password_hash(mod.password_hash, password):
            return jsonify({"is_mod": True}), 200
        else:
            return jsonify({"error": "Invalid credentials"}), 401

    except Exception as e:
        print(traceback.format_exc())
        return jsonify({"error": "Internal server error"}), 500

