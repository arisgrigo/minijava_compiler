import java.util.*;

public class symbolTable {
    LinkedHashMap<String, Classes> classMap;
    LinkedHashMap<String, offset> offsetMap;       //I use LinkedHashMap instead of HashMap, so the offset prints the classes in the order of their declaration
                                                    //The rest of the HashMaps, are also linked, for the sake of uniformity


    public symbolTable(){
        this.classMap = new LinkedHashMap<String, Classes>();
        this.offsetMap = new LinkedHashMap<String, offset>();
    }

    public String addClass(String className, Classes parentClass){
        if(classMap.containsKey(className))
            return null;

        Classes insertedClass = new Classes(className, parentClass);
        classMap.put(className, insertedClass);                     //insert the created class into the symbol table's class hash map
        return className;
    }

    public Classes getClass(String className){
      if(!classMap.containsKey(className)){
          return null;
      }else return classMap.get(className);
    }

    //adds parent class to the class that was given
    public String addClassExtension(String className, String parentName){
        if(!classMap.containsKey(parentName)){
            return null;
        }

        Classes insertedClassParent = this.getClass(parentName);
        Classes insertedClass = new Classes(className, insertedClassParent);
        classMap.put(className, insertedClass);

        return className;
    }
}

class offset {
    public String offsetName;         //is the offset's class
    public String print;              //contains the text that gets printed for every class
    public int variableOffset;
    public int methodOffset;

    offset(String offsetName){
        this.offsetName = offsetName;
        this.print = null;
        this.variableOffset = 0;
        this.methodOffset = 0;
    }

    void addPrint(String offsetLine){
        if(this.print == null){
            this.print = offsetLine;
        }else this.print += offsetLine;
    }

    void printOffset(){
        System.out.print(this.print);
    }
}