public interface Exportable {

    String toCsv();

    default void printExport() {
        System.out.println(toCsv());
    }
}