package dashflight.sparkbootstrap;

public class Location {

    private int id;
    private String name;

    public Location(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static Location withFields(Integer id, String name) {
        if (id == null || name == null) {
            return null;
        }

        return new Location(id, name);
    }
}
