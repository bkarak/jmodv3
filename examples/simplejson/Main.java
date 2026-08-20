package examples.simplejson;

public class Main {
    public static void main(String[] args) {
        Person person = new Person("Ada", 36, new String[] {"math", "poet"});
        System.out.println(person.toJson());
    }
}
