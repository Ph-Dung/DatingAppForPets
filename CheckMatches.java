import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckMatches {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/petmatch_db";
        String user = "postgres";
        String password = "123456";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            // Check if matches exist between 303 and 304
            String sql = "SELECT id, user1_id, user2_id, matched_at FROM matches";
            ResultSet rs = stmt.executeQuery(sql);

            boolean foundAny = false;
            while (rs.next()) {
                foundAny = true;
                int id = rs.getInt("id");
                int user1 = rs.getInt("user1_id");
                int user2 = rs.getInt("user2_id");
                System.out.println("Match ID: " + id + ", User1: " + user1 + ", User2: " + user2);
            }
            if (!foundAny) {
                System.out.println("No matches found in the database at all!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
