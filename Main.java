import syntaxtree.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws Exception {
        if(args.length < 1){
            System.err.println("Usage: java Main <inputFile>");
            System.exit(1);
        }

        FileInputStream fis = null;
        try{
            for(String arg: args){
                fis = new FileInputStream(arg);
                MiniJavaParser parser = new MiniJavaParser(fis);

                Goal root = parser.Goal();

                System.err.println("Program parsed successfully.");

                FirstVisitor eval = new FirstVisitor();

                symbolTable symTable = new symbolTable();
                root.accept(eval, symTable);                //FirstVisitor fills the symbol table, checks for double declarations, and calculates the offset of each class

                SecondVisitor eval2 = new SecondVisitor();
                root.accept(eval2, symTable);               //SecondVisitor used the filled symbol table, to find other error (like wrong assignments, wrong types etc.)

                Set<String> keySet = symTable.offsetMap.keySet();

                //prints the offset of each class (in order of declaration)
                for(String key: keySet){
                    offset tmpOffset = symTable.offsetMap.get(key);
                    if(tmpOffset.print != null){
                        tmpOffset.printOffset();
                    }
                }
            }
        }
        catch(ParseException ex){
            System.out.println(ex.getMessage());
        }
        catch(FileNotFoundException ex){
            System.err.println(ex.getMessage());
        }
        finally{
            try{
                if(fis != null) fis.close();
            }
            catch(IOException ex){
                System.err.println(ex.getMessage());
            }
        }
    }
}
