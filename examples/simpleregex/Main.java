package examples.simpleregex;

public class Main {
    public static void main(String[] args) {
        IpAddress ip = new IpAddress();
        String sample = args.length > 0 ? args[0] : "127.0.0.1";
        System.out.println(ip.matches(sample));
    }
}
