public class Student extends User {

    private String course;

    public Student(int id, String name, String email, String course) {
        super(id, name, email);
        this.course = course;
    }

    public String getCourse() {
        return course;
    }

    @Override
    public String role() {
        return "STUDENT";
    }

    @Override
    public String permissions() {
        return "read only";
    }

    @Override
    public String toCsv() {
        return super.toCsv() + "," + course;
    }
}