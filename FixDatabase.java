import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class FixDatabase {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/petmatch_db";
        String user = "postgres";
        String password = "123456";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            // Drop constraint
            System.out.println("Dropping constraint messages_type_check...");
            try {
                stmt.execute("ALTER TABLE messages DROP CONSTRAINT IF EXISTS messages_type_check");
                stmt.execute("ALTER TABLE messages DROP CONSTRAINT IF EXISTS messages_type_check1");
                System.out.println("Success.");
            } catch (Exception e) {
                System.out.println("Error dropping constraint: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
