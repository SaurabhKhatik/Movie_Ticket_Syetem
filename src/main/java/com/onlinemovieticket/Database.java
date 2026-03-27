package com.onlinemovieticket;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple JDBC layer using an embedded file-based SQL database (H2).
 */
public final class Database {
  private static final String DB_DIR = "data";
  private static final String DB_FILE = "movietickets";
  private static final String JDBC_URL;
  private static final String JDBC_USER = "sa";
  private static final String JDBC_PASS = "";

  private static volatile boolean initialized = false;

  static {
    Path dbPath = Path.of(DB_DIR, DB_FILE).toAbsolutePath().normalize();
    // H2 URL expects forward slashes.
    String dbPathForUrl = dbPath.toString().replace("\\", "/");
    JDBC_URL = "jdbc:h2:file:" + dbPathForUrl + ";MODE=MySQL;AUTO_SERVER=TRUE";
  }

  private Database() {}

  public static void init() {
    if (initialized) return;
    synchronized (Database.class) {
      if (initialized) return;
      try {
        Files.createDirectories(Path.of(DB_DIR).toAbsolutePath().normalize());

        // Ensure driver class is available in older setups.
        Class.forName("org.h2.Driver");

        try (Connection conn = getConnection()) {
          try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS movies ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "title VARCHAR(200) NOT NULL, "
                + "genre VARCHAR(100), "
                + "language VARCHAR(60), "
                + "duration_minutes INT NOT NULL, "
                + "price_per_seat INT NOT NULL, "
                + "poster_url VARCHAR(255)"
                + ")");
            st.execute("ALTER TABLE movies ADD COLUMN IF NOT EXISTS poster_url VARCHAR(255)");

            st.execute("CREATE TABLE IF NOT EXISTS bookings ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "movie_id INT NOT NULL, "
                + "show_date DATE NOT NULL, "
                + "show_time VARCHAR(10) NOT NULL, "
                + "seats INT NOT NULL, "
                + "customer_name VARCHAR(120) NOT NULL, "
                + "customer_email VARCHAR(120) NOT NULL, "
                + "customer_phone VARCHAR(30), "
                + "total_price INT NOT NULL, "
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()"
                + ")");
            st.execute("CREATE TABLE IF NOT EXISTS users ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "full_name VARCHAR(120) NOT NULL, "
                + "email VARCHAR(120) NOT NULL UNIQUE, "
                + "password_hash VARCHAR(255) NOT NULL, "
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()"
                + ")");

            // Seed sample movies once.
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) AS cnt FROM movies")) {
              int cnt = 0;
              if (rs.next()) cnt = rs.getInt("cnt");
              if (cnt == 0) seedSampleMovies(conn);
            }
            updatePostersForExistingRows(conn);
          }
        }

        initialized = true;
      } catch (Exception e) {
        throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
      }
    }
  }

  private static void seedSampleMovies(Connection conn) throws SQLException {
    // Add sample movies for the dashboard.
    insertMovie(conn, "Inception", "Sci-Fi", "English", 148, 500, "/images/inception.svg");
    insertMovie(conn, "Avengers: Endgame", "Action", "English", 181, 600, "/images/endgame.svg");
    insertMovie(conn, "Spirited Away", "Animation", "Japanese", 125, 450, "/images/spirited-away.svg");
    insertMovie(conn, "KGF Chapter 2", "Action", "Kannada", 168, 550, "/images/kgf-2.svg");
    insertMovie(conn, "Dangal", "Drama", "Hindi", 161, 400, "/images/dangal.svg");
  }

  private static void insertMovie(Connection conn, String title, String genre, String language, int durationMinutes,
      int pricePerSeat, String posterUrl) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(
        "INSERT INTO movies(title, genre, language, duration_minutes, price_per_seat, poster_url) VALUES(?,?,?,?,?,?)")) {
      ps.setString(1, title);
      ps.setString(2, genre);
      ps.setString(3, language);
      ps.setInt(4, durationMinutes);
      ps.setInt(5, pricePerSeat);
      ps.setString(6, posterUrl);
      ps.executeUpdate();
    }
  }

  private static void updatePostersForExistingRows(Connection conn) throws SQLException {
    String selectSql = "SELECT id, title FROM movies WHERE poster_url IS NULL OR TRIM(poster_url) = ''";
    String updateSql = "UPDATE movies SET poster_url = ? WHERE id = ?";
    try (PreparedStatement sel = conn.prepareStatement(selectSql);
        PreparedStatement upd = conn.prepareStatement(updateSql);
        ResultSet rs = sel.executeQuery()) {
      while (rs.next()) {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        upd.setString(1, getPosterForTitle(title));
        upd.setInt(2, id);
        upd.addBatch();
      }
      upd.executeBatch();
    }
  }

  private static String getPosterForTitle(String title) {
    if (title == null) return "/images/default-poster.svg";
    String t = title.trim().toLowerCase();
    if (t.contains("inception")) return "/images/inception.svg";
    if (t.contains("endgame")) return "/images/endgame.svg";
    if (t.contains("spirited")) return "/images/spirited-away.svg";
    if (t.contains("kgf")) return "/images/kgf-2.svg";
    if (t.contains("dangal")) return "/images/dangal.svg";
    return "/images/default-poster.svg";
  }

  public static List<Movie> getAllMovies() {
    init();
    List<Movie> out = new ArrayList<>();
    String sql = "SELECT id, title, genre, language, duration_minutes, price_per_seat, poster_url FROM movies ORDER BY id";
    try (Connection conn = getConnection(); Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      while (rs.next()) {
        Movie m = new Movie(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("genre"),
            rs.getString("language"),
            rs.getInt("duration_minutes"),
            rs.getInt("price_per_seat"),
            rs.getString("poster_url"));
        out.add(m);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to load movies: " + e.getMessage(), e);
    }
    return out;
  }

  public static Movie getMovieById(int id) {
    init();
    String sql = "SELECT id, title, genre, language, duration_minutes, price_per_seat, poster_url FROM movies WHERE id = ?";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new Movie(
              rs.getInt("id"),
              rs.getString("title"),
              rs.getString("genre"),
              rs.getString("language"),
              rs.getInt("duration_minutes"),
              rs.getInt("price_per_seat"),
              rs.getString("poster_url"));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to load movie: " + e.getMessage(), e);
    }
    return null;
  }

  public static int createBooking(int movieId, LocalDate showDate, String showTime, int seats, String customerName,
      String customerEmail, String customerPhone, int totalPrice) {
    init();
    String sql = "INSERT INTO bookings(movie_id, show_date, show_time, seats, customer_name, customer_email, customer_phone, total_price, created_at) "
        + "VALUES(?,?,?,?,?,?,?,?,?)";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, movieId);
      ps.setDate(2, Date.valueOf(showDate));
      ps.setString(3, showTime);
      ps.setInt(4, seats);
      ps.setString(5, customerName);
      ps.setString(6, customerEmail);
      ps.setString(7, customerPhone);
      ps.setInt(8, totalPrice);
      ps.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
      ps.executeUpdate();

      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) return rs.getInt(1);
      }
      throw new RuntimeException("Booking inserted but ID not returned.");
    } catch (SQLException e) {
      throw new RuntimeException("Failed to create booking: " + e.getMessage(), e);
    }
  }

  private static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS);
  }

  public static boolean userExistsByEmail(String email) {
    init();
    String sql = "SELECT id FROM users WHERE LOWER(email) = LOWER(?)";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, email);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed checking existing user: " + e.getMessage(), e);
    }
  }

  public static int createUser(String fullName, String email, String passwordHash) {
    init();
    String sql = "INSERT INTO users(full_name, email, password_hash, created_at) VALUES(?,?,?,?)";
    try (Connection conn = getConnection();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, fullName);
      ps.setString(2, email);
      ps.setString(3, passwordHash);
      ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) return rs.getInt(1);
      }
      throw new RuntimeException("User inserted but ID not returned.");
    } catch (SQLException e) {
      throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
    }
  }

  public static User findUserByEmailAndPasswordHash(String email, String passwordHash) {
    init();
    String sql = "SELECT id, full_name, email FROM users WHERE LOWER(email) = LOWER(?) AND password_hash = ?";
    try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, email);
      ps.setString(2, passwordHash);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new User(rs.getInt("id"), rs.getString("full_name"), rs.getString("email"));
        }
      }
      return null;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to authenticate user: " + e.getMessage(), e);
    }
  }

  public static final class Movie {
    public final int id;
    public final String title;
    public final String genre;
    public final String language;
    public final int durationMinutes;
    public final int pricePerSeat;
    public final String posterUrl;

    public Movie(int id, String title, String genre, String language, int durationMinutes, int pricePerSeat,
        String posterUrl) {
      this.id = id;
      this.title = title;
      this.genre = genre;
      this.language = language;
      this.durationMinutes = durationMinutes;
      this.pricePerSeat = pricePerSeat;
      this.posterUrl = posterUrl;
    }
  }

  public static final class User {
    public final int id;
    public final String fullName;
    public final String email;

    public User(int id, String fullName, String email) {
      this.id = id;
      this.fullName = fullName;
      this.email = email;
    }
  }
}

