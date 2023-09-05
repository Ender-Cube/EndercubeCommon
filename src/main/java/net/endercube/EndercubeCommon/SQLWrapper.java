package net.endercube.EndercubeCommon;

import com.google.gson.JsonObject;
import com.zaxxer.hikari.HikariDataSource;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.mojang.MojangUtils;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * An abstracted interface for SQL designed for Endercube
 */
public class SQLWrapper {

    private final HikariDataSource dataSource;

    /**
     * An abstracted interface for SQL designed for Endercube
     * @param dataSource the database to use
     * @throws SQLException when SQL fails
     */
    public SQLWrapper(HikariDataSource dataSource) throws SQLException {
        this.createTable();
        this.dataSource = dataSource;
    }

    /**
     * Adds a time to the database
     *
     * @param player The player the time belongs to
     * @param course The {@link String} id of the course to look up
     * @param time   The time in milliseconds
     * @throws SQLException when SQL fails
     */
    public void addTime(Player player, String course, Long time) throws SQLException {
        Connection connection = dataSource.getConnection();
        String sql = "INSERT INTO playerTimes(player,course,time) VALUES(?,?,?)";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, String.valueOf(player.getUuid()));
        preparedStatement.setString(2, course);
        preparedStatement.setLong(3, time);
        preparedStatement.executeUpdate();
        connection.close();
    }

    /**
     * Gets the specified players best times ordered by an index
     *
     * @param player player to retrieve data from
     * @param course The {@link String} id of the course to look up
     * @param index  time to get, 1 for best
     * @return the nth best time of that player
     * @throws SQLException when SQL fails
     */
    @Nullable
    public Long getTimePlayer(Player player, String course, int index) throws SQLException {
        Connection connection = dataSource.getConnection();
        String sql = "SELECT * FROM playerTimes WHERE player = ? AND course = ? ORDER BY time ASC LIMIT 1 OFFSET ?;";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, String.valueOf(player.getUuid()));
        preparedStatement.setString(2, course);
        preparedStatement.setInt(3, index - 1);
        ResultSet resultSet = preparedStatement.executeQuery();
        connection.close();

        while (resultSet.next()) {
            return resultSet.getLong("time");
        }

        // Return null if we found nothing
        return null;
    }

    /**
     * Gets the specified players best times ordered by an index
     *
     * @param UUID  player's UUID to retrieve data from
     * @param course The {@link String} id of the course to look up
     * @param index time to get, 1 for best
     * @throws SQLException when SQL fails
     * @return the nth best time of that player
     */
    @Nullable
    public Long getTimeUUID(String UUID, String course, int index) throws SQLException {
        Connection connection = dataSource.getConnection();
        String sql = "SELECT * FROM playerTimes WHERE player = ? AND course = ? ORDER BY time ASC LIMIT 1 OFFSET ?;";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, UUID);
        preparedStatement.setString(2, course);
        preparedStatement.setInt(3, index - 1);
        ResultSet resultSet = preparedStatement.executeQuery();
        connection.close();

        while (resultSet.next()) {
            return resultSet.getLong("time");
        }

        // Return null if we found nothing
        return null;
    }

    /**
     * Retrieves the overall nth best time for a course
     *
     * @param course The course to get data for
     * @param index  The nth time you want
     * @return The time
     * @throws SQLException when SQL fails
     */
    @Nullable
    public Long getTimeOverall(String course, int index) throws SQLException {
        Connection connection = dataSource.getConnection();
        String sql = "SELECT * FROM playerTimes WHERE course = ? ORDER BY time ASC LIMIT 1 OFFSET ?;";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, course);
        preparedStatement.setInt(2, index - 1);
        ResultSet resultSet = preparedStatement.executeQuery();
        connection.close();

        while (resultSet.next()) {
            return resultSet.getLong("time");
        }

        // Return null if we found nothing
        return null;
    }

    /**
     * Retrieves the overall nth best player for a course
     *
     * @param course the course to get data for
     * @param index  The nth time you want
     * @return the player's name
     * @throws SQLException when SQL fails
     */
    @Nullable
    public String getPlayerOverall(String course, int index) throws SQLException {
        Connection connection = dataSource.getConnection();
        String sql = "SELECT * FROM playerTimes WHERE course = ? ORDER BY time ASC LIMIT 1 OFFSET ?;";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, course);
        preparedStatement.setInt(2, index - 1);
        ResultSet resultSet = preparedStatement.executeQuery();
        connection.close();

        JsonObject playerNameObject = MojangUtils.fromUuid(resultSet.getString("player"));

        if (playerNameObject == null) {
            return null;
        }

        return playerNameObject
                .get("name")
                .getAsString();
    }

    /**
     * Removes all but the top ten times per player for all players
     * @throws SQLException when SQL fails
     */
    public void pruneDatabase() throws SQLException {
        Connection connection = dataSource.getConnection();

        // Thanks, ChatGPT <3
        String sql = """
                DELETE FROM playerTimes
                  WHERE (player, course, time) NOT IN (
                    SELECT player, course, time
                    FROM (
                      SELECT player, course, time,
                             ROW_NUMBER() OVER (PARTITION BY player, course ORDER BY time ASC) AS row_num
                      FROM playerTimes
                    ) AS subQuery
                    WHERE row_num <= 10
                  );
                """;

        connection.createStatement().execute(sql);
        connection.close();
    }

    /**
     * Creates the table - called on class initialisation
     * @throws SQLException when SQL fails
     */
    private void createTable() throws SQLException {
        Connection connection = dataSource.getConnection();
        String createTable = """
                CREATE TABLE IF NOT EXISTS playerTimes (
                    player text NOT NULL,
                    course text NOT NULL,
                    time bigint NOT NULL
                    
                );
                """;

        Statement statement = connection.createStatement();
        statement.execute(createTable);
        connection.close();
    }
}
