package dashflight.sparkbootstrap;

public class Organization {

    private int id;
    private String name;

    public Organization(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}