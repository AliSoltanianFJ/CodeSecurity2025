// ...existing code...
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    if err := r.ParseForm(); err != nil {
        http.Error(w, "invalid form", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }
    if len(password) < 8 {
        http.Error(w, "password too short", http.StatusBadRequest)
        return
    }

    // Argon2id parameters suitable for interactive logins; tune for your environment.
    const (
        memory      = 64 * 1024 // 64 MB
        iterations  = 3
        parallelism = 4
        saltLen     = 16
        keyLen      = 32
    )

    salt := make([]byte, saltLen)
    if _, err := rand.Read(salt); err != nil {
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    hash := argon2.IDKey([]byte(password), salt, iterations, memory, uint8(parallelism), keyLen)

    // Encode parameters, salt and hash into a single string for storage.
    b64Salt := base64.RawStdEncoding.EncodeToString(salt)
    b64Hash := base64.RawStdEncoding.EncodeToString(hash)
    encoded := fmt.Sprintf("$argon2id$v=19$m=%d,t=%d,p=%d$%s$%s", memory, iterations, parallelism, b64Salt, b64Hash)

    // NOTE: update DSN to match your environment (don't hardcode secrets in production)
    db, err := sql.Open("mysql", "root:password@tcp(127.0.0.1:3306)/testdb")
    if err != nil {
        http.Error(w, "database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    _, err = db.Exec(`CREATE TABLE IF NOT EXISTS users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(255) NOT NULL UNIQUE,
        password_hash TEXT NOT NULL
    )`)
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }

    stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, encoded)
    if err != nil {
        http.Error(w, "could not create user (maybe username exists)", http.StatusConflict)
        return
    }

    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "user created")
}
// ...existing code...
