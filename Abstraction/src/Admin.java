public class Admin extends User {

    public Admin(int id, String name, String email) {
        super(id, name, email);
    }

    @Override
    public String role() {
        return "ADMIN";
    }

    @Override
    public String permissions() {
        return "create, read, update, delete";
    }
}