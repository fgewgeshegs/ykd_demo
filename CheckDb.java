import java.sql.*;
import java.io.File;

public class CheckDb {
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Exception e) {
            System.err.println("Failed to load driver: " + e);
        }
    }

    public static void main(String[] args) throws Exception {
        File dataDir = new File("./data");
        File[] dbs = dataDir.listFiles((d, name) -> name.endsWith(".db"));
        System.out.println("=== Database files ===");
        for (File f : dbs) {
            System.out.println("  " + f.getName() + " (" + f.length() + " bytes)");
        }

        System.out.println("\n=== claw-memories.db ===");
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:./data/claw-memories.db")) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"});
            while (tables.next()) {
                System.out.println("  表: " + tables.getString("TABLE_NAME"));
            }

            System.out.println("\n  user_memory 表数据:");
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM user_memory")) {
                while (rs.next()) {
                    System.out.println("    id=" + rs.getLong("id")
                        + " user_id=" + rs.getString("user_id")
                        + " type=" + rs.getString("memory_type")
                        + " key=" + rs.getString("memory_key")
                        + " value=" + rs.getString("memory_value")
                        + " confidence=" + rs.getDouble("confidence")
                        + " updated=" + rs.getString("updated_time"));
                }
            }
            System.out.println("  ---");
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM user_memory")) {
                rs.next();
                System.out.println("  总数: " + rs.getInt(1));
            }

            System.out.println("\n  user_profile 表数据:");
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM user_profile")) {
                while (rs.next()) {
                    System.out.println("    id=" + rs.getLong("id")
                        + " user_id=" + rs.getString("user_id")
                        + " key=" + rs.getString("key")
                        + " value=" + rs.getString("value")
                        + " updated=" + rs.getString("updated_at"));
                }
            }
            System.out.println("  ---");
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM user_profile")) {
                rs.next();
                System.out.println("  总数: " + rs.getInt(1));
            }
        }

        System.out.println("\n=== claw-profiles.db ===");
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:./data/claw-profiles.db")) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"});
            while (tables.next()) {
                System.out.println("  表: " + tables.getString("TABLE_NAME"));
            }

            System.out.println("\n  profile_summary 表数据:");
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM profile_summary")) {
                while (rs.next()) {
                    System.out.println("    id=" + rs.getLong("id")
                        + " user_id=" + rs.getString("user_id")
                        + " type=" + rs.getString("profile_type")
                        + " key=" + rs.getString("profile_key")
                        + " value=" + rs.getString("profile_value")
                        + " updated=" + rs.getString("updated_time"));
                }
            }
            System.out.println("  ---");
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM profile_summary")) {
                rs.next();
                System.out.println("  总数: " + rs.getInt(1));
            }
        }
    }
}