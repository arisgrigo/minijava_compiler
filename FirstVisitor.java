import syntaxtree.*;
import visitor.*;

//FirstVisitor fills the symbol table, checks for double declarations, and calculates the offset of each class
public class FirstVisitor extends GJDepthFirst<String, symbolTable>{
    public Classes currentClass;            //keeps the track of which class the program is currently checking
    public Methods currentMethod;           //keeps the track of which method the program is currently checking (null if the checking is outside of a method)

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, symbolTable symTable) throws Exception {
        String className = n.f1.accept(this, symTable);
        if(symTable.addClass(className, null) == null){
            System.out.printf("error: '%s' class has already been declared!\n", className);
            throw new Exception("error: "+ className + " class has already been declared!");
        }
        //currentClass is updated, so we know what class we are currently checking
        currentClass = symTable.getClass(className);

        //create offset for the new class, and put it in the offset Hash Map
        offset classOffset = new offset(className);
        symTable.offsetMap.put(className, classOffset);

        String methodReturns = n.f5.toString();
        String methodName = n.f6.toString();            //will always be "main"
        if(currentClass.addMethod(methodName, methodReturns) == null){
            System.out.print("error: Main method has already been declared!\n");
            throw new Exception("error: Main method has already been declared!");
        }
        //currentMethod is updated, so we know what method we are currently checking
        currentMethod = currentClass.getMethod(methodName);

        n.f11.accept(this, symTable);
        n.f14.accept(this, symTable);

	currentClass = null;
	currentMethod = null;

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, symbolTable symTable) throws Exception {
        String className = n.f1.accept(this, symTable);
        if(symTable.addClass(className, null) == null) {
            System.out.printf("error: '%s' class has already been declared\n", className);
            throw new Exception("error: " + className + " class has already been declared!");
        }

        offset classOffset = new offset(className);
        symTable.offsetMap.put(className, classOffset);

        currentClass = symTable.getClass(className);

	n.f3.accept(this, symTable);
	n.f4.accept(this, symTable);

	currentClass = null;
	currentMethod = null;

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, symbolTable symTable) throws Exception {
        String className = n.f1.accept(this, symTable);
        String parentName = n.f3.accept(this, symTable);

        if(symTable.addClass(className, null) == null) {
            System.out.printf("error '%s' class has already been declared\n", className);
            throw new Exception("error: " + className + " class has already been declared!");
        }

        //check to see if parent class has not been declared yet. If it has, add it as parent class to the current class
        if(symTable.addClassExtension(className, parentName) == null) {
            System.out.printf("error: '%s' (parent class) for '%s' has not been declared!\n", parentName, className);
            throw new Exception("error: " + parentName  + " (parent class) for " + className + " has not been declared!");
        }

        currentClass = symTable.getClass(className);
        offset classOffset = new offset(className);
        symTable.offsetMap.put(className, classOffset);

	n.f5.accept(this, symTable);
	n.f6.accept(this, symTable);


	currentClass = null;
	currentMethod = null;

        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, symbolTable symTable) throws Exception {
	String methodType = n.f1.accept(this, symTable);
        String methodName = n.f2.accept(this, symTable);

        if(currentClass.addMethod(methodName, methodType) == null){
            System.out.printf("error: '%s' method has already been declared in '%s' class \n", methodName, currentClass.className);
            throw new Exception("error: " + methodName + " method has already been declared in " + currentClass.className + " class!");
        }
        currentMethod = currentClass.getMethod(methodName);
        String argumentList = n.f4.present() ? n.f4.accept(this, symTable) : "";

        //this block of code is here to check if current method is overriding a method from a parent class
        int isOverriding = 0;
        Classes parentClass;
        argumentList = currentMethod.parameterTypeMap.toString();
        if(currentClass.parentClass != null) {
            Classes tmpClass = currentClass;
            parentClass = currentClass.parentClass;
            while(tmpClass != null){
                Methods parentMethod = parentClass.getMethod(methodName);
                if(parentMethod != null){
                    String argumentList2 = parentMethod.parameterTypeMap.toString();
                    if(parentMethod.methodName.equals(currentMethod.methodName)){
                        if(!argumentList.equals(argumentList2)){
                            System.out.printf("error: '%s' method has already been declared in '%s' class and is not overriding\n", methodName, parentClass.className);
                            throw new Exception("error: " + methodName + " method has already been declared in " + parentClass.className + " class and is not overriding!");
                        }else isOverriding = 1;
                    }
                }
                parentClass = parentClass.parentClass;
                tmpClass = parentClass;
            }
        }

        //if method is not overriding, then it must be included in class's offset
        if(isOverriding == 0){
            String parentClassName = currentClass.className;
            Classes tmpClass = currentClass;

            if(currentClass.parentClass != null){
                parentClassName = tmpClass.parentClass.className;
                while(tmpClass.parentClass != null){
                    parentClassName = tmpClass.parentClass.className;
                    tmpClass = tmpClass.parentClass;
                }
            }

            offset tmpOffset = symTable.offsetMap.get(currentClass.className);
            offset parentOffset = symTable.offsetMap.get(parentClassName);

            tmpOffset.addPrint(currentClass.className + "." + currentMethod.methodName+ " : " + parentOffset.methodOffset + "\n");

            parentOffset.methodOffset += 8;
        }

        n.f7.accept(this, symTable);
	n.f8.accept(this, symTable);

	currentMethod = null;
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, symbolTable symTable) throws Exception {
        String ret = n.f0.accept(this, null);

        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n, symbolTable symTable) throws Exception {
        return n.f1.accept(this, symTable);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, symbolTable symTable) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += ", " + node.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, symbolTable symTable) throws Exception{
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);
        if(currentMethod.addParameter(name, type) == null){
            System.out.printf("error: '%s' variable has already been declared in parameter list of '%s' method!\n", name, currentMethod.methodName);
            throw new Exception("error: "+ name+" variable has already been declared in parameter list of " + currentMethod.methodName + " method!");
        }
        currentMethod.parameterTypeMap.add(type);
        currentMethod.parameterMap.put(name, type);
        return type + " " + name;
    }

	/**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
	@Override
	public String visit(VarDeclaration n, symbolTable symTable) throws Exception {
      String variableType = n.f0.accept(this, symTable);
      String variableName = n.f1.accept(this, symTable);

      if(currentMethod == null){                                    //if the variable is declared outside of a method
          if(currentClass.addVariable(variableName, variableType) == null){
              System.out.printf("error: '%s' variable has already been declared in '%s' class!\n", variableName, currentClass.className);
              throw new Exception("error: " + variableName + " variable has already been declared in " + currentClass.className+ " class!");
          }
      }else{                                                        //if the variable is declared inside of a method
          if(currentMethod.addVariable(variableName, variableType) == null){
              System.out.printf("error: '%s' variable has already been declared in '%s' method!\n", variableName, currentMethod.methodName);
              throw new Exception("error: " + variableName + " variable has already been declared in " + currentMethod.methodName + " method!");
          }
      }

      //if variable is declared outside of a method, then it must be added to the class's offset
      if(currentMethod == null) {
          String parentClassName = currentClass.className;
          Classes tmpClass = currentClass;

          if (currentClass.parentClass != null) {
              parentClassName = tmpClass.parentClass.className;
              while (tmpClass.parentClass != null) {
                  parentClassName = tmpClass.parentClass.className;
                  tmpClass = tmpClass.parentClass;
              }
          }

          offset parentOffset = symTable.offsetMap.get(parentClassName);

          offset tmpOffset = symTable.offsetMap.get(currentClass.className);
          tmpOffset.addPrint(currentClass.className + "." + variableName + " : " + parentOffset.variableOffset + "\n");

          if (variableType.equals("boolean")) {
              parentOffset.variableOffset += 1;
          }
          if (variableType.equals("int")) {
              parentOffset.variableOffset += 4;
          }
          if (!variableType.equals("boolean") && !variableType.equals("int")) {
              parentOffset.variableOffset += 8;
          }
      }
      variableName = variableName + ' ' +  n.f1.accept(this, symTable);

      return variableName;
   }

    public String visit(ArrayType n, symbolTable symTable) {
        return "int[]";
    }

    public String visit(BooleanType n, symbolTable symTable) {
        return "boolean";
    }

    public String visit(IntegerType n, symbolTable symTable) {
        return "int";
    }

    public String visit(Identifier n, symbolTable symTable) {
        return n.f0.toString();
    }
}
