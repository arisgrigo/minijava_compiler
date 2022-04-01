import syntaxtree.*;
import visitor.*;
import java.util.ArrayList;

public class SecondVisitor extends GJDepthFirst<String, symbolTable>{
    public Classes currentClass;                            //keeps the track of which class the program is currently checking
    public Methods currentMethod;                           //keeps the track of which method the program is currently checking (null if the checking is outside of a method)
    ArrayList<ArrayList<String>> expressionLists;           //a list of lists, each list holds the parameters of a method (used in ExpressionList)
    int depth = 0;                                          //used in sendMessage, in tandem with the expressionLists, in the event of a method, which calls a method, which calls a method etc.

    public SecondVisitor(){
        this.expressionLists = new ArrayList<ArrayList<String>>();
    }

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
    public String visit(MainClass n, symbolTable symTable) throws Exception {
        String className = n.f1.accept(this, symTable);

        currentClass = symTable.getClass(className);
        currentMethod = currentClass.getMethod("main");             //first class will always contain the method "main"

        n.f5.accept(this, symTable);
        n.f6.accept(this, symTable);
        n.f11.accept(this, symTable);
        n.f14.accept(this, symTable);
        n.f15.accept(this, symTable);

        currentMethod = null;
        currentClass = null;

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
    public String visit(ClassDeclaration n, symbolTable symTable) throws Exception {
        String className = n.f1.accept(this, symTable);

        currentClass = symTable.getClass(className);

        n.f3.accept(this, symTable);
        n.f4.accept(this, symTable);

        currentMethod = null;
        currentClass = null;

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
    public String visit(ClassExtendsDeclaration n, symbolTable symTable) throws Exception {
        String className = n.f1.accept(this, symTable);
        String parentName = n.f3.accept(this, symTable);

        currentClass = symTable.getClass(className);

        n.f5.accept(this, symTable);
        n.f6.accept(this, symTable);

        currentMethod = null;
        currentClass = null;

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
    public String visit(MethodDeclaration n, symbolTable symTable) throws Exception {
        String methodType = n.f1.accept(this, symTable);
        String methodName = n.f2.accept(this, symTable);

        currentMethod = currentClass.getMethod(methodName);

        String argumentList = n.f4.present() ? n.f4.accept(this, symTable) : "";

        n.f7.accept(this, symTable);
        n.f8.accept(this, symTable);

        String returns = n.f10.accept(this, symTable);

        Classes tmpClass = null;

        if(!returns.equals(methodType)){
            tmpClass = symTable.getClass(returns);          //check to see if return type is a parent class
            if(tmpClass != null){
                tmpClass = tmpClass.parentClass;
                while(tmpClass != null){
                    if(methodType.equals(tmpClass.className)){
                        break;
                    }
                    tmpClass = tmpClass.parentClass;
                }
            }
        }

        //if tmpClass == null, then that means that the above block of code, never found a parent class with the same name as the method return type
        if(tmpClass == null && !returns.equals(methodType)){
            System.out.printf("error: return type ('%s') does not correspond to method's return type ('%s') in '%s' method!\n", returns, methodType, methodName);
            throw new Exception("error: return type ("+ returns +") does not correspond to method's return type ("+ methodType +") in " + methodName +" method!");
        }

        currentMethod = null;
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterList n, symbolTable symTable) throws Exception {
        String ret = n.f0.accept(this, symTable);

        if (n.f1 != null) {
            ret += n.f1.accept(this, symTable);
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
    public String visit(FormalParameterTail n, symbolTable symTable) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += ", " + node.accept(this, symTable);
        }

        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n, symbolTable symTable) throws Exception{
        String type = n.f0.accept(this, symTable);
        String name = n.f1.accept(this, symTable);

        return type + " " + name;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, symbolTable symTable) throws Exception {
        String variableType = n.f0.accept(this, symTable);
        String variableName = n.f1.accept(this, symTable);

        //now that the symbol table is filled, we can check if the variable is declared as a class that has actually been declared
        if(!variableType.equals("boolean") && !variableType.equals("int") & !variableType.equals("int[]") && symTable.getClass(variableType) == null){
            System.out.printf("error: '%s' variable needs to be one of allowed types: boolean, int, int[] or a declared class but is '%s'!\n", variableName,variableType);
            throw new Exception("error: "+ variableName +" variable needs to be one of allowed types: boolean, int, int[] or a declared class but is " + variableType +"!");
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

    // /**
    // * f0 -> AndExpression()
    // *       | CompareExpression()
    // *       | PlusExpression()
    // *       | MinusExpression()
    // *       | TimesExpression()
    // *       | ArrayLookup()
    // *       | ArrayLength()
    // *       | MessageSend()
    // *       | Clause()
    // */
    public String visit(Expression n, symbolTable symTable) throws Exception {
        return n.f0.accept(this, symTable);
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, symbolTable symTable) throws Exception {
        String value0 = n.f0.accept(this, symTable);
        String value1 = n.f2.accept(this, symTable);
        if(value0.equals("boolean") && value1.equals("boolean")){
            return "boolean";
        }
        System.out.printf("error: And ('&&') Expression with wrong types ('%s') and ('%s')!\n", value0, value1);
        throw new Exception("error: And ('&&') Expression with wrong types " + value0 + " and "+ value1+ "!");
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, symbolTable symTable) throws Exception {
        String value0 = n.f0.accept(this, symTable);
        String value1 = n.f2.accept(this, symTable);
        if(value0.equals("int") && value1.equals("int")){
            return "boolean";
        }
        System.out.printf("error: Compare ('<') Expression with wrong types ('%s') and ('%s')!\n", value0, value1);
        throw new Exception("error: Compare ('<') Expression with wrong types " + value0 + " and "+ value1+ "!");
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, symbolTable symTable) throws Exception {
        String value0 = n.f0.accept(this, symTable);
        String value1 = n.f2.accept(this, symTable);
        if(value0.equals("int") && value1.equals("int")){
            return "int";
        }
        System.out.printf("error: Plus ('+') Expression with wrong types ('%s') and ('%s')!\n", value0, value1);
        throw new Exception("error: Plus ('+') Expression with wrong types " + value0 + " and "+ value1+ "!");
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, symbolTable symTable) throws Exception {
        String value0 = n.f0.accept(this, symTable);
        String value1 = n.f2.accept(this, symTable);
        if(value0.equals("int") && value1.equals("int")){
            return "int";
        }
        System.out.printf("error: Minus ('-') Expression with wrong types ('%s') and ('%s')!\n", value0, value1);
        throw new Exception("error: Minus ('-') Expression with wrong types " + value0 + " and "+ value1+ "!");
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, symbolTable symTable) throws Exception {
        String value0 = n.f0.accept(this, symTable);
        String value1 = n.f2.accept(this, symTable);
        if(value0.equals("int") && value1.equals("int")){
            return "int";
        }
        System.out.printf("error: Times ('*') Expression with wrong types ('%s') and ('%s')!\n", value0, value1);
        throw new Exception("error: Times ('*') Expression with wrong types " + value0 + " and "+ value1+ "!");
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, symbolTable symTable) throws Exception {
        String value0 = n.f0.accept(this, symTable);
        String value1 = n.f2.accept(this, symTable);
        if(value0.equals("int[]") && value1.equals("int")){
            return "int";
        }

        if(!value0.equals("int[]")){
            System.out.printf("error: wrong type ('%s') in array lookup!\n", value0);
            throw new Exception("error: wrong type (" + value0 + ") in array lookup!");
        }

        if(!value1.equals("int")){
            System.out.printf("error: wrong type ('%s') in array index!\n", value1);
            throw new Exception("error: wrong type (" + value1 + ") in array index!");
        }

        throw new Exception("incorrect type in ArrayLookup!");
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, symbolTable symTable) throws Exception {
        String type = n.f0.accept(this, symTable);
        if(type.equals("int[]")){
            return "int";
        }
        System.out.printf("error: Only int[] can make use of .length but '%s' was used!\n", type);
        throw new Exception("error: Only int[] can make use of .length but " + type + " was used!");
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( ExpressionList() )?
     * f5 -> ")"
     */
    public String visit(MessageSend n, symbolTable symTable) throws Exception {
        String className = n.f0.accept(this, symTable);
        if(symTable.getClass(className) == null){
            System.out.printf("error: '%s' class has not yet been declared!\n", className);
            throw new Exception("error: " + className + " class has not yet been delcared!");
        }
        Classes tmpClass = symTable.getClass(className);
        String methodName = n.f2.accept(this, symTable);
        Methods tmpMethod = tmpClass.methodMap.get(methodName);
        if(tmpMethod == null){
            if(tmpClass != null){
                if(tmpClass.parentClass != null){
                    tmpClass = tmpClass.parentClass;
                    while(tmpClass != null){
                        if(tmpClass.methodMap.get(methodName) != null){
                            tmpMethod = tmpClass.methodMap.get(methodName);
                            break;
                        }
                        tmpClass = tmpClass.parentClass;
                    }
                }
            }
        }

        //if tmpMethod is null, it means that the above block of code never found a method of a parent with the same name as the method is MessageSend
        if(tmpMethod == null){
            System.out.printf("error: '%s' class does not contain '%s' method!\n", className, methodName);
            throw new Exception("error: " + className + " class does not contain " + methodName + " method!");
        }

        //check to see if method's parameters are the same with the parameters given to it
        String methodParameters = tmpMethod.parameterTypeMap.toString();
        String parameters = n.f4.accept(this, symTable);

        if(parameters == null && !methodParameters.equals("[]")){
            System.out.printf("error: wrong parameters given in '%s' method of '%s' class!\n", currentMethod.methodName, currentClass.className);
            throw new Exception("error: wrong parameters given in "+ currentMethod.methodName  + " method of " + currentClass.className +" class!");
        }

        //if they are not, check to see if some parameter is a class that extends the correct one
        if(parameters != null) {
            int length = parameters.length();
            String classTmp = parameters.substring(1,length-1);                 //get rid of brackets from returned set
            int length2 = methodParameters.length();
            String classTmp2 = methodParameters.substring(1, length2-1);;       //get rid of brackets from returned set
            Classes tmpClass1 = symTable.getClass(classTmp);

            if(tmpClass1 != null){
                Classes parentClass = tmpClass1.parentClass;
                Methods tmpMethod1;
                while(parentClass != null){
                    classTmp = parentClass.className;
                    tmpMethod1 = parentClass.getMethod(methodName);
                    if(classTmp.equals(classTmp2)){
                        parameters = methodParameters;
                    }
                    parentClass = parentClass.parentClass;
                }
            }
            if(!parameters.equals(methodParameters)){
                System.out.printf("error: wrong parameters given in '%s' method of '%s' class!\n", currentMethod.methodName, currentClass.className);
                throw new Exception("error: wrong parameters given in "+ currentMethod.methodName  + " method of " + currentClass.className +" class!");
            }
        }

        return tmpMethod.returns;
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n, symbolTable symTable) throws Exception {
        //depth increases and decreases recursively, as one method calls another
        int curDepth = depth++;
        //create and add the expression list to the list of lists
        ArrayList<String> newList = new ArrayList<String>();
        this.expressionLists.add(newList);
        String start = n.f0.accept(this, symTable);

        this.expressionLists.get(curDepth).add(start);

        String tail = n.f1.accept(this, symTable);

        //ret contains the fully formed expression list of the current depth's method parameters
        String ret = this.expressionLists.get(curDepth).toString();
        if(ret == null){
            return "[]";            //empty array, meaning that the method had no parameters
        }
        //clear the list so it can be used in future calls
        this.expressionLists.get(curDepth).clear();
        depth--;
        return ret;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public String visit(ExpressionTail n, symbolTable symTable) throws Exception {
        return n.f0.accept(this, symTable);
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public String visit(ExpressionTerm n, symbolTable symTable) throws Exception {
        String tail = n.f1.accept(this, symTable);
        this.expressionLists.get(depth-1).add(tail);
        return tail;
    }

    /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | NotExpression()
    *       | BracketExpression()
    */
    public String visit(PrimaryExpression n, symbolTable symTable) throws Exception {
        String value = n.f0.accept(this, symTable);

        String type = currentMethod.getVarOrPar(value);

        if(value == null){
            System.out.print("error: A value is required!\n");
            throw new Exception("error: A value is required!");
        }

        if(value.equals("boolean")){
            return "boolean";
        }

        if(value.equals("int")){
            return "int";
        }

        Classes tmpClass = symTable.getClass(value);
        if(tmpClass != null){
            if(symTable.getClass(value) == null){
                System.out.printf("error: '%s' class has not been declared!\n", value);
                throw new Exception("error: " + value +" class has not been declared!");
            }
            return value;
        }

        if(value.equals("int[]")){
            return "int[]";
        }

        String variableType = currentMethod.getVarOrPar(value);         //if value is nothing of the above, then it must be a variable
        if(variableType == null){
            variableType = currentClass.variableMap.get(value);         //look if variable has been declared outside the method (either in class, or class' parent)

            if(variableType == null){
                Classes parentClass = currentClass.parentClass;
                if(parentClass != null) {
                    tmpClass = currentClass.parentClass;
                    while(tmpClass != null){
                        if(tmpClass.variableMap.get(value) != null){
                            variableType = tmpClass.variableMap.get(value);
                            break;
                        }
                        tmpClass = tmpClass.parentClass;
                    }
                }
            }
        }
        if(variableType == null){
            System.out.printf("error: '%s' variable  has not been declared yet in '%s' method!\n", value, currentMethod.methodName);
            throw new Exception("error: "+ value + " variable  has not been declared yet in " + currentMethod.methodName + " method!");
        }
        return variableType;
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, symbolTable symTable) throws Exception {
        return "int";
    }

    /**
     * f0 -> "true"
     */
    public String visit(TrueLiteral n, symbolTable symTable) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n, symbolTable symTable) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, symbolTable symTable) throws Exception {
        return n.f0.toString();
    }


    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n, symbolTable symTable) throws Exception {
        return currentClass.className;
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(ArrayAllocationExpression n, symbolTable symTable) throws Exception {
        String typeCheck = n.f3.accept(this, symTable);

        if(!typeCheck.equals("int")){
            System.out.printf("error: type given is '%s' but 'int' is required, in array!\n", typeCheck);
            throw new Exception("error: type given is " + typeCheck + " but 'int' is required, in array!");
        }
        return "int[]";
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public String visit(AllocationExpression n, symbolTable symTable) throws Exception {
        String className = n.f1.accept(this, symTable);
        if(symTable.getClass(className) == null){
            System.out.printf("error: '%s' class has not been declared!\n", className);
            throw new Exception("error: " + className + " class has not been declared!");
        }

        return className;
    }

    /**
     * f0 -> "!"
     * f1 -> PrimaryExpression()
     */
    public String visit(NotExpression n, symbolTable symTable) throws Exception {
        String typeCheck = n.f1.accept(this, symTable);
        if(typeCheck.equals("boolean")){
            return "boolean";
        }
        System.out.printf("error: NotExpression ('!') with wrong type ('%s')!\n", typeCheck);
        throw new Exception("error: NotExpression ('!') with wrong type ("+ typeCheck +")!");
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, symbolTable symTable) throws Exception {
        return n.f1.accept(this, symTable);
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, symbolTable symTable) throws Exception {
        String assignment = n.f0.accept(this, symTable);
        String type = n.f2.accept(this, symTable);

        String typeCheck = currentMethod.getVarOrPar(assignment);
        if(typeCheck == null){
            typeCheck = currentClass.variableMap.get(assignment);

            if(typeCheck == null){
                if(currentClass.parentClass != null){
                    String parent = currentClass.parentClass.className;
                    Classes tmpClass = symTable.getClass(parent);
                    while(tmpClass != null){
                        if(tmpClass.variableMap.get(assignment) != null){
                            typeCheck = tmpClass.variableMap.get(assignment);
                            break;
                        }
                        tmpClass = tmpClass.parentClass;
                    }
                }
            }
        }

        if(typeCheck == null){
            System.out.printf("error: '%s' variable has not been declared!\n", assignment);
            throw new Exception("error: " + assignment + " variable has not been declared!");
        }

        boolean flag = false;
        if(!typeCheck.equals(type)){                        //check if type is a parent class
            Classes tmpClass = symTable.getClass(type);
            if(tmpClass != null){
                Classes tmpParentClass = tmpClass.parentClass;
                if(tmpParentClass != null){
                    if(!tmpParentClass.className.equals(typeCheck)){
                        System.out.printf("error: incorrect type matching! Assigned '%s' to '%s' in '%s' method!\n", type, typeCheck, currentMethod.methodName);
                        throw new Exception("error: incorrect type matching! Assigned " + type + " to " + typeCheck + " in method: " + currentMethod.methodName);
                    }else flag = true;
                }
            }
        }

        if(!flag && !typeCheck.equals(type)){
            System.out.printf("error: incorrect type matching! Assigned '%s' to '%s' in '%s' method!\n", type, typeCheck, currentMethod.methodName);
            throw new Exception("error: incorrect type matching! Assigned " + type + " to " + typeCheck + " in method: " + currentMethod.methodName);
        }

        return "AssignmentStatement";
    }

    /**
     * f0 -> Identifier()
     * f1 -> "["
     * f2 -> Expression()
     * f3 -> "]"
     * f4 -> "="
     * f5 -> Expression()
     * f6 -> ";"
     */
    public String visit(ArrayAssignmentStatement n, symbolTable symTable) throws Exception {
        String assignment = n.f0.accept(this, symTable);

        String typeCheck = currentMethod.getVarOrPar(assignment);
        if(typeCheck == null){
            typeCheck = currentClass.variableMap.get(assignment);
            if(typeCheck == null){
                Classes tmpClass = currentClass.parentClass;
                while(tmpClass != null){
                    if(tmpClass.variableMap.get(assignment) != null){
                        typeCheck = tmpClass.variableMap.get(assignment);           //typeCheck becomes the type of the assignment (which was declared in a parent class)
                        break;
                    }
                    tmpClass = tmpClass.parentClass;
                }
            }
        }

        if(typeCheck == null){
            System.out.printf("error: '%s' variable has not been declared!\n", assignment);
            throw new Exception("error: " + assignment + " variable has not been declared!");
        }

        if(!typeCheck.equals("int[]")){
            System.out.print("error: incorrect type matching!\n");
            throw new Exception("error: incorrect type matching!");
        }

        String indexNumber = n.f2.accept(this, symTable);
        if(!indexNumber.equals("int")){
            System.out.print("error: index of int array must be int!\n");
            throw new Exception("error: index of int array must be int!");
        }

        String givenNumber = n.f5.accept(this, symTable);
        if(!givenNumber.equals("int")){
            System.out.print("error: index of int array can only receive int!\n");
            throw new Exception("error: index of int array can only receive int!");
        }

        return "arrayAssignmentStatement";
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public String visit(IfStatement n, symbolTable symTable) throws Exception {
        String typeCheck = n.f2.accept(this, symTable);

        if(typeCheck.equals("int")) {
            System.out.print("error: While statement requires boolean, but int was given!\n");
            throw new Exception("error: While statement requires boolean, but int was given!");
        }

        if(!typeCheck.equals("boolean")) {
            System.out.printf("error: If statement requires boolean, but '%s' was given!\n", typeCheck);
            throw new Exception("error: If statement requires boolean , but +" + typeCheck +  " was given!");
        }

        n.f4.accept(this, symTable);
        n.f6.accept(this, symTable);

        return "ifStatement";
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public String visit(WhileStatement n, symbolTable symTable) throws Exception {
        String typeCheck = n.f2.accept(this, symTable);

        if(typeCheck.equals("int")) {
            System.out.print("error: While statement requires boolean, but int was given!\n");
            throw new Exception("error: While statement requires boolean, but int was given!");
        }

        if(!typeCheck.equals("boolean")) {
            System.out.printf("error: While statement requires boolean, but '%s' was given!\n", typeCheck);
            throw new Exception("error: While statement requires boolean, but +" + typeCheck +  " was given!");
        }

        n.f4.accept(this, symTable);

        return "whileStatement";
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public String visit(PrintStatement n, symbolTable symTable) throws Exception {
        String typeCheck = n.f2.accept(this, symTable);

        if(typeCheck.equals("boolean") || typeCheck.equals("int") ) {
            return "printStatement";
        }
        System.out.printf("error: Print statement requires boolean or int, but was given '%s'!\n", typeCheck);
        throw new Exception("error: Print statement requires boolean or int, but was given "+ typeCheck + "!");
    }

}
