import java.io.FileReader;
import java.io.IOException;

public class JFlexScanner {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java JFlexScanner <inputfile>");
            return;
        }
        try {
            Yylex scanner = new Yylex(new FileReader(args[0]));
            Token token;
            System.out.println("ZenLang JFlex Scanner  —  scanning: " + args[0]);
            System.out.println("==================================================================================");
            System.out.println("");
            System.out.println("==================================================================================");
            System.out.println("TOKEN STREAM");
            System.out.println("==================================================================================");
            
            while ((token = scanner.yylex()) != null) {
                System.out.println(token);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
