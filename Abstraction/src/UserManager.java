import java.util.ArrayList;

public class UserManager {

    private ArrayList<User> users = new ArrayList<>();

    public void add(User user) {
        users.add(user);
        System.out.println("Added: " + user.getName() + " (" + user.role() + ")");
    }

    public void listAll() {
        if (users.isEmpty()) {
            System.out.println("No users yet.");
            return;
        }

        String header =
                "ID NAME EMAIL ROLE PERMISSIONS";
        System.out.println(header);
        System.out.println("-".repeat(69));

        for (User u : users) {
            u.display();
        }

        System.out.println("Total users: " + users.size());
    }

    public User findById(int id) {
        for (User u : users) {
            if (u.getId() == id) {
                return u;
            }
        }

        return null;
    }

    public boolean deleteById(int id) {
        User found = findById(id);

        if (found == null) {
            return false;
        }

        users.remove(found);
        return true;
    }

    public void exportAll() {
        if (users.isEmpty()) {
            System.out.println("Nothing to export.");
            return;
        }

        System.out.println("--- CSV EXPORT ---");

        for (User u : users) {
            u.printExport();
        }

        System.out.println("--- END OF EXPORT ---");
    }

    public int count() {
        return users.size();
    }
}