-- Tempo schema. Portable: runs unchanged on H2 (MySQL mode) and MySQL 8.
-- The app executes this file automatically at startup (every statement is IF NOT EXISTS).

CREATE TABLE IF NOT EXISTS users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(200) NOT NULL,
  role VARCHAR(10) NOT NULL DEFAULT 'USER',
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_user_role CHECK (role IN ('ADMIN', 'USER'))
);

-- Admin-configurable goal parameters (goal type + tracking metric + target cap)
CREATE TABLE IF NOT EXISTS goal_types (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(60) NOT NULL UNIQUE,
  metric VARCHAR(100) NOT NULL,
  max_target_hours INT NOT NULL DEFAULT 500
);

CREATE TABLE IF NOT EXISTS goals (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  goal_type_id INT NULL,
  name VARCHAR(100) NOT NULL,
  target_hours DECIMAL(7,2) NOT NULL,
  deadline DATE NOT NULL,
  status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_goal_status CHECK (status IN ('ACTIVE', 'COMPLETED')),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (goal_type_id) REFERENCES goal_types(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS time_logs (
  id INT AUTO_INCREMENT PRIMARY KEY,
  goal_id INT NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  duration_minutes INT NOT NULL,
  CONSTRAINT chk_log_times CHECK (end_time > start_time),
  FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE CASCADE
);
