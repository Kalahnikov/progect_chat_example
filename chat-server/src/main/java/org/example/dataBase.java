package org.example;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class dataBase {
    private static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String SELECT_USERS =
            "select u.id as id, u.login as login, u.pass as pass, u.user_name as user_name, r.role_name as role_name  from user_to_roles utr\n" +
                    "\tjoin users u on user_id=u.id \n" +
                    "\tjoin roles r on role_id=r.id";
    private static final String SELECT_USER_WITH_ROLE =
            "select u.id as id, u.login as login, u.pass as pass, u.user_name as user_name, r.role_name as role_name  from user_to_roles utr\n" +
                    "            join users u on user_id=u.id \n" +
                    "            join roles r on role_id=r.id\n" +
                    "            where r.role_name = ?;";
    private static final String INSERT_USER = "insert into public.users(id, login, pass, user_name)\n" +
            "\tvalues(?, ?, ?, ?);";
    private static final String INSERT_USER_TO_ROLES = "insert into public.user_to_roles (user_id, role_id)\n" +
            "\tvalues(?,1);";
    private static final String SELECT_MAX_USER_ID = "SELECT id FROM public.users u  WHERE id=(select max(id) from public.users u2)";
    private static final String UPDATE_USERNAME = "UPDATE public.users SET user_name  = ? WHERE user_name  = ?;";
    private static final String UPDATE_BAN_USER = "UPDATE public.users SET ban_user  = ?:: timestamp\n" +
            "\tWHERE user_name = ?;";
    private static final String SELECT_BAN_USER_DATE = "SELECT ban_user  FROM public.users u  WHERE user_name = ?;";

    public List<User> selectUsers() {
        List<User> users = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "kalashnikov", "kalashnikov")) {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_USERS)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt(1);
                        String log = rs.getString(2);
                        String pass = rs.getString(3);
                        String userName = rs.getString(4);
                        String role = rs.getString(5);
                        User user = new User(id, log, pass, userName, role);
                        users.add(user);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return users;
    }

    public List<User> selectUsersWithRole(String role) {
        List<User> users = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "kalashnikov", "kalashnikov")) {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_USER_WITH_ROLE)) {
                ps.setString(1, role);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt(1);
                        String log = rs.getString(2);
                        String pass = rs.getString(3);
                        String userName = rs.getString(4);
                        String role1 = rs.getString(5);
                        User user = new User(id, log, pass, userName, role1);
                        users.add(user);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return users;
    }

    public int insertUser(String login, String password, String userName) {
        // /register morty mor iii
        int id;
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "kalashnikov", "kalashnikov")) {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_MAX_USER_ID)) {
                try (ResultSet rs = ps.executeQuery()) {
                    id = rs.getInt(1);
                }
            }
            try (PreparedStatement ps1 = connection.prepareStatement(INSERT_USER)) {
                ps1.setInt(1, id + 1);
                ps1.setString(2, login);
                ps1.setString(3, password);
                ps1.setString(4, userName);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = connection.prepareStatement(INSERT_USER_TO_ROLES)) {
                ps2.setInt(1, id + 1);
                ps2.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return id + 1;
    }

    public void setUpdateUsername(String userName, String userNameNow) {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "kalashnikov", "kalashnikov")) {
            try (PreparedStatement ps = connection.prepareStatement(UPDATE_USERNAME)) {
                ps.setString(1, userNameNow);
                ps.setString(2, userName);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setUpdateBanUser(String massage, String time) {
        String[] mes = massage.split(" ", 2);
        String[] t = time.split(" ", 5);
        LocalDateTime dateTime = LocalDateTime.of(Integer.parseInt(t[0]), Integer.parseInt(t[1]), Integer.parseInt(t[2]), Integer.parseInt(t[3]), Integer.parseInt(t[4]), 0);
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "kalashnikov", "kalashnikov")) {
            try (PreparedStatement ps = connection.prepareStatement(UPDATE_BAN_USER)) {
                ps.setObject(1, dateTime);
                ps.setString(2, mes[1]);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Timestamp setBanUserDate(String userName) {
        Timestamp timestamp = null;
        ArrayList<Timestamp> timestamps = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "kalashnikov", "kalashnikov")) {
            try (PreparedStatement ps = connection.prepareStatement(SELECT_BAN_USER_DATE)) {
                ps.setString(1, userName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        timestamp = rs.getTimestamp(1);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return timestamp;
    }
}
