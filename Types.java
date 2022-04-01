import java.util.*;

class Classes {
    String className;
    Classes parentClass;                            //is null if the class doesn't have a parent
    LinkedHashMap<String, String> variableMap;      //contains each variable that has been declared inside of the class (but not inside of a method)
    LinkedHashMap<String, Methods> methodMap;       //contains each method that has been declared inside of the class

    public Classes(String className, Classes parentClass){
        this.className = className;
        this.parentClass = parentClass;
        this.variableMap = new LinkedHashMap<String, String>();
        this.methodMap = new LinkedHashMap<String, Methods>();
    }

    public String addMethod(String methodName, String returns){
        if(methodMap.containsKey(methodName))
            return null;

        Methods insertedMethod = new Methods(methodName, returns);
        methodMap.put(methodName, insertedMethod);
        return methodName;
    }

    public Methods getMethod(String methodName){
        if(!methodMap.containsKey(methodName)){
            return null;
        }else return methodMap.get(methodName);
    }

    //a variable consists of its name, and its type (both are strings)
    public String addVariable(String variableName, String variableType){
        if(variableMap.containsKey(variableName))
            return null;

        variableMap.put(variableName, variableType);
        return variableName;
    }


}

class Methods {
    String methodName;
    String returns;
    LinkedHashMap<String, String> variableMap;          //contains each variable declared inside the method
    LinkedHashMap<String, String> parameterMap;         //contains each variable declared inside the method's parameters (like public int foo(int j, boolean k))
    ArrayList<String> parameterTypeMap;                 //contains every type from the method's parameters (for above example it would be [int, boolean])

    public Methods(String methodName, String returns){
        this.methodName = methodName;
        this.returns = returns;
        this.parameterMap = new LinkedHashMap<String, String>();
        this.variableMap = new LinkedHashMap<String, String>();
        this.parameterTypeMap = new ArrayList<String>();
    }

    public String addVariable(String variableName, String variableType){
        //checks if variable is declared either in method's parameters or method's variables
        if(variableMap.containsKey(variableName) || parameterMap.containsKey(variableName))
            return null;

        variableMap.put(variableName, variableType);
        return variableName;
    }

    public String addParameter(String parameterName, String parameterType){
        if(parameterMap.containsKey(parameterName))
            return null;

        parameterMap.put(parameterName, parameterType);
        return parameterName;
    }

    //returns a method's specific variable or parameter
    public String getVarOrPar(String variableName){
        if(variableMap.containsKey(variableName)){
            return variableMap.get(variableName);     //returns variable's type
        }
        if(parameterMap.containsKey(variableName)){
            return parameterMap.get(variableName);     //returns variable's type
        }
        return null;
    }
}
