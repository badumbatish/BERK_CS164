package chocopy.pa3;

import chocopy.common.analysis.AbstractNodeAnalyzer;
import chocopy.common.analysis.SymbolTable;
import chocopy.common.analysis.types.ClassValueType;
import chocopy.common.analysis.types.FuncType;
import chocopy.common.analysis.types.Type;
import chocopy.common.astnodes.*;
import chocopy.common.codegen.*;

import java.util.List;

import static chocopy.common.codegen.RiscVBackend.Register.*;

public class CodeGenImpl extends CodeGenBase {

    public CodeGenImpl(RiscVBackend backend) {
        super(backend);
        isStrCat = false;
        isStrEq = false;
        isListCat = false;
        isListCons = false;
        isBoxChar = false;
    }

//    emitStdFunc(boxChar, "boxchar", "chocopy/custom/");
//    emitStdFunc(boxInt, "boxint", "chocopy/custom/");
//    emitStdFunc(boxBool, "boxbool", "chocopy/custom/");
//    emitStdFunc(strcat, "strcat", "chocopy/custom/");
//    emitStdFunc(streq, "streq", "chocopy/custom/");
//
//    emitListConstruction(boxList);
//    emitListConcat(listConcatFunc);
//    emitErrorFunc(errorNone, "Operation on None", ERROR_NONE);
//    emitErrorFunc(errorDiv, "Division by zero", ERROR_DIV_ZERO);
//    emitErrorFunc(errorOob, "Index out of bounds", ERROR_OOB);
    private boolean isStrCat;
    private boolean isStrEq;
    private boolean isBoxChar;
    private boolean isListCat;
    private  boolean isListCons;


    private final Label errorDiv = new Label("error.Div");
    private final Label errorOob = new Label("error.OOB");
    private final Label errorNone = new Label("error.None");


    private final Label boxInt = new Label("impl.boxInt");
    private final Label boxBool = new Label("impl.boxBool");
    private final Label boxChar = new Label("impl.boxChar");
    private final Label strcat = new Label("strcat");
    private final Label streq = new Label("streq");

    private final Label boxList = new Label("impl.boxList");
    private final Label boxListLoading = new Label("impl.boxListLoading");
    private final Label boxListDone = new Label("impl.boxListDone");

    private final Label listConcatFunc = new Label("impl.listConcat");
    private final int HeaderSlots = 4;

    protected void emitTopLevel(List<Stmt> statements) {
        StmtAnalyzer stmtAnalyzer = new StmtAnalyzer(null);
        backend.emitADDI(SP, SP, -2 * backend.getWordSize(),
                "Saved FP and saved RA (unused at top level).");
        backend.emitSW(ZERO, SP, 0, "Top saved FP is 0.");
        backend.emitSW(ZERO, SP, 4, "Top saved RA is 0.");
        backend.emitADDI(FP, SP, 2 * backend.getWordSize(),
                "Set FP to previous SP.");

        for (Stmt stmt : statements) {
            stmt.dispatch(stmtAnalyzer);
        }
        backend.emitLI(A0, EXIT_ECALL, "Code for ecall: exit");
        backend.emitEcall(null);
    }

    protected void emitUserDefinedFunction(FuncInfo funcInfo) {
        backend.emitGlobalLabel(funcInfo.getCodeLabel());

        backend.emitSW(RA, SP, -4, "");
        backend.emitSW(FP, SP, -8, "");
        backend.emitADDI(SP, SP, -8, "");


        backend.emitADDI(FP, SP, 8, "Fp is now at old sp");
        StmtAnalyzer stmtAnalyzer = new StmtAnalyzer(funcInfo);

        stmtAnalyzer.handleFunctionLocals(funcInfo);
        stmtAnalyzer.allStmts(funcInfo.getStatements());

        backend.emitMV(A0, ZERO, "Returning None implicitly");
        backend.emitLocalLabel(stmtAnalyzer.epilogue, "Epilogue");

        stmtAnalyzer.popFunctionLocals(funcInfo);

        backend.emitMV(SP, FP, "Sp is now at old sp");
        backend.emitLW(FP, SP, -8, "");
        backend.emitLW(RA, SP, -4, "");

        backend.emitJR(RA, "Return to caller");
    }

    public void push(RiscVBackend.Register register, String comment) {
        backend.emitADDI(SP, SP, -4, "push stack for " + comment);
        backend.emitSW(register, SP, 0, "store " + comment);
    }

    public void pop(RiscVBackend.Register register, String comment) {
        backend.emitLW(register, SP, 0, "pop stack for " + comment);
        backend.emitADDI(SP, SP, +4, "load " + comment);
    }


    private class BinaryExprHandler {
        StmtAnalyzer stmtAnalyzer;
        BinaryExpr node;
        BinaryExprHandler (StmtAnalyzer stmtAnalyzer, BinaryExpr binaryExpr) {
            this.stmtAnalyzer = stmtAnalyzer;
            node = binaryExpr;
        }

        boolean isIntLiteral(Expr expr) {
            return expr instanceof IntegerLiteral;
        }



        void handleAND() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "AND left operand");

            Label end = generateLocalLabel();

            backend.emitBEQZ(A0, end, "AND short circuit");

            node.right.dispatch(stmtAnalyzer);
            pop(A1, "AND left operand");

            backend.emitAND(A0, A1, A0, "AND");

            backend.emitLocalLabel(end, "AND end");
        }

        void handleOR() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "OR left operand");

            Label end = generateLocalLabel();

            backend.emitBNEZ(A0, end, "OR short circuit");

            node.right.dispatch(stmtAnalyzer);
            pop(A1, "OR left operand");

            backend.emitOR(A0, A1, A0, "OR");

            backend.emitLocalLabel(end, "OR short circuit");
        }

        void handlePlus() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "PLUS left operand");

            node.right.dispatch(stmtAnalyzer);
            pop(A1, "PLUS left operand");

            backend.emitADD(A0, A1, A0, "PLUS");
        }

        void handleStrConcat() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "STRCAT left operand");

            node.right.dispatch(stmtAnalyzer);
            backend.emitMV(A1, A0, "STRCAT right operand");

            pop(A0, "STRCAT left operand");

            backend.emitJAL(strcat, "STRCAT");
            isStrCat = true;
        }

        void handleListConcat() {
            node.left.dispatch(stmtAnalyzer);

            push(A0, "LISTCAT left operand");

            node.right.dispatch(stmtAnalyzer);
            backend.emitMV(A1, A0, "LISTCAT right operand");
            pop(A0, "LISTCAT left operand");

            backend.emitLW(A4, A0, 12, "");
            backend.emitADD(A2, ZERO, A4, "");
            backend.emitLW(A4, A1, 12, "");
            backend.emitADD(A2, A2, A4, "");
            backend.emitJAL(listConcatFunc, "LISTCAT");
            isListCat = true;
        }

        void handleMinus() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "MINUS left operand");

            node.right.dispatch(stmtAnalyzer);
            pop(A1, "MINUS left operand");

            backend.emitSUB(A0, A1, A0, "MINUS");
        }

        void handleMul() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "MUL left operand");

            node.right.dispatch(stmtAnalyzer);
            pop(A1, "MUL right operand");

            backend.emitMUL(A0, A1, A0, "MUL");
        }

        void handleDiv () {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "DIV left operand");

            node.right.dispatch(stmtAnalyzer);
            pop(A1, "DIV left operand");

            Label ok = generateLocalLabel();
            backend.emitBNEZ(A0, ok, "DIV zero check");
            backend.emitJ(errorDiv, "Zero check failed");
            backend.emitLocalLabel(ok, "DIV zero check");

            backend.emitDIV(A0, A1, A0, "DIV");
        }

        void handleMod() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "MUL left operand");

            node.right.dispatch(stmtAnalyzer);
            pop(A1, "MUL left operand");

            Label ok = generateLocalLabel();
            backend.emitBNEZ(A0, ok, "MOD zero check");
            backend.emitJ(errorDiv, "Zero check failed");
            backend.emitLocalLabel(ok, "MOD zero check");

            backend.emitInsn("rem a0, a1, a0", "MOD");
        }

        void handleLess() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "LT left operand");

            node.right.dispatch(stmtAnalyzer);
            pop(A1, "LT left operand");

            backend.emitSLT(A0, A1, A0, "LT");
        }

        public void handleLessEqual() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "LTE left operand");

            node.right.dispatch(stmtAnalyzer);
            pop(A1, "LTE left operand");

            // A1 <= A0
            // !(A1 > A0)
            // !(A0 < A1)

            backend.emitSLT(A0, A0, A1, "LTE (right < left)");
            backend.emitSEQZ(A0, A0, "LTE");
        }

        public void handleGreater() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "GT left operand");

            node.right.dispatch(stmtAnalyzer);
            pop(A1, "GT left operand");

            // A1 > A0
            // A0 < A1

            backend.emitSLT(A0, A0, A1, "GT");
        }

        public void handleGreaterEqual() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "GTE left operand");

            node.right.dispatch(stmtAnalyzer);
            pop(A1, "GTE left operand");

            // A1 >= A0
            // !(A1 < A0)
            // !(A0 > A1)

            backend.emitSLT(A0, A1, A0, "GTE (left < right)");
            backend.emitSEQZ(A0, A0, "GTE");
        }

        public void handleIntBoolEqual() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "EQ left operand");

            node.right.dispatch(stmtAnalyzer);
            pop(A1, "EQ left operand");

            backend.emitSUB(A0, A1, A0, "EQ (left - right)");
            backend.emitSEQZ(A0, A0, "EQ");
        }

        public void handleNotEqual() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "NEQ left operand");

            node.right.dispatch(stmtAnalyzer);
            pop(A1, "NEQ left operand");

            backend.emitSUB(A0, A1, A0, "NEQ (left - right)");
            backend.emitSNEZ(A0, A0, "NEQ");
        }

        public void handleStrEqual() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "STREQ left operand");

            node.right.dispatch(stmtAnalyzer);
            backend.emitMV(A1, A0, "STREQ right operand");
            pop(A0, "STREQ left operand");

            backend.emitJAL(streq, "STREQ");
            isStrEq = true;
        }

        public void handleStrNotEqual() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "STRNEQ left operand");

            node.right.dispatch(stmtAnalyzer);
            backend.emitMV(A1, A0, "STRNEQ right operand");
            pop(A0, "STRNEQ left operand");

            backend.emitJAL(streq, "STRNEQ");
            isStrEq = true;
            backend.emitSEQZ(A0, A0, "STRNEQ negate");
        }

        public void handleIs() {
            node.left.dispatch(stmtAnalyzer);
            push(A0, "IS left operand");

            node.right.dispatch(stmtAnalyzer);
            pop(A1, "IS left operand");

            backend.emitSUB(A0, A1, A0, "IS (left - right)");
            backend.emitSEQZ(A0, A0, "IS");
        }

        public void handle() {
            switch (node.operator) {
                case "and":
                    this.handleAND();
                    break;
                case "or":
                    this.handleOR();
                    break;
                case "+":
                    if (node.left.getInferredType().isListType() && node.right.getInferredType().isListType()) {
                        this.handleListConcat();
                    } else if (node.left.getInferredType().equals(Type.STR_TYPE)) {
                        this.handleStrConcat();
                    } else {
                        this.handlePlus();
                    }
                    break;
                case "-":
                    this.handleMinus();
                    break;
                case "*":
                    this.handleMul();
                    break;
                case "//":
                    this.handleDiv();
                    break;
                case "%":
                    this.handleMod();
                    break;
                case "<":
                    this.handleLess();
                    break;
                case "<=":
                    this.handleLessEqual();
                    break;
                case ">":
                    this.handleGreater();
                    break;
                case ">=":
                    this.handleGreaterEqual();
                    break;
                case "==":
                    if (node.left.getInferredType().equals(Type.STR_TYPE)) {
                        this.handleStrEqual();
                    } else {
                        this.handleIntBoolEqual();
                    }
                    break;
                case "!=":
                    if (node.left.getInferredType().equals(Type.STR_TYPE)) {
                        this.handleStrNotEqual();
                    } else {
                        this.handleNotEqual();
                    }
                    break;
                case "is":
                    this.handleIs();
                    break;
            }
        }
    }

    private class StmtAnalyzer extends AbstractNodeAnalyzer<Void> {

        private final SymbolTable<SymbolInfo> sym;
        protected Label epilogue;

        /**
         * The descriptor for the current function, or null at the top
         * level.
         */
        private final FuncInfo funcInfo;
        StmtAnalyzer(FuncInfo funcInfo0) {
            funcInfo = funcInfo0;
            if (funcInfo == null) {
                sym = globalSymbols;
            } else {
                sym = funcInfo.getSymbolTable();
            }
            epilogue = generateLocalLabel();
        }

        private void boxIfRequired(Type sourceType, Type targetType) {
            if (sourceType.equals(Type.INT_TYPE) && targetType.equals(Type.OBJECT_TYPE)) {
                backend.emitJAL(boxInt, "");
            }
            if (sourceType.equals(Type.BOOL_TYPE) && targetType.equals(Type.OBJECT_TYPE)) {
                backend.emitJAL(boxBool, "");
            }
        }

        void handleClassCreation(CallExpr node) {
            ClassInfo ci = (ClassInfo) sym.get(node.function.name);

            if (ci.getClassName().equals("int")) {
                backend.emitLI(A0, 0, "int()");
                return;
            }
            if (ci.getClassName().equals("bool")) {
                backend.emitLI(A0, 0, "bool()");
                return;
            }

            backend.emitLA(A0, ci.getPrototypeLabel(), "Load prototype label");
            backend.emitJAL(objectAllocLabel, "Allocate object");

            push(A0, "Push constructor argument");

            backend.emitLW(A1, A0, 8, "Load the address of dispatch table");
            backend.emitLW(A1, A1, 0, "Load the address of init func");
            backend.emitJALR(A1, "Jump to init");

            pop(A0, "Pop constructor argument");
        }

        /**
         * Handle class field. If store is null, loads, otherwise stores that register into the field.
         */
        private void handleClassField(MemberExpr node, RiscVBackend.Register store) {
            node.object.dispatch(this);
            noneCheck(A0, "Object should not be None");

            String className = node.object.getInferredType().className();
            ClassInfo ci = (ClassInfo) sym.get(className);
            int offset = getAttrOffset(ci, node.member.name);

            if (store == null) {
                backend.emitLW(A0, A0, offset, String.format("Load %s.%s", className, node.member.name));
            } else {
                backend.emitSW(store, A0, offset, String.format("Store %s.%s", className, node.member.name));
            }
        }
        @Override
        public Void analyze(MemberExpr node) {
            handleClassField(node, null);
            return null;
        }
        @Override
        public Void analyze(MethodCallExpr node) {
            handleStaticLink(node);
            handleCallExprArgs(node);

            String funcName = node.method.member.name;
            String className = node.method.object.getInferredType().className();
            ClassInfo classInfo = (ClassInfo) sym.get(className);

            backend.emitLW(A0, A0, 8, String.format("%s: dispatch table", className));
            backend.emitLW(A0, A0, getMethodOffset(classInfo, funcName), String.format("%s.%s: method address", className, funcName));

            backend.emitJALR(A0, String.format("%s.%s: call", className, funcName));

            popCallExprArgs(node);
            popStaticLink(node);
            return null;
        }
        @Override
        public Void analyze(CallExpr node) {
            SymbolInfo s = sym.get(node.function.name);
            if (s instanceof ClassInfo) {
                handleClassCreation(node);
                return null;
            }

            FuncInfo f = (FuncInfo) s;

            handleStaticLink(node);
            handleCallExprArgs(node);

            backend.emitJAL(f.getCodeLabel(), "Function invocation");

            popCallExprArgs(node);
            popStaticLink(node);

            return null;
        }

        @Override
        public Void analyze(AssignStmt node) {
            node.value.dispatch(this);
            push(S2, "Reserve S2");
            backend.emitMV(S2, A0, "Copy value");
            for (Expr target : node.targets) {
                if (target instanceof Identifier) {
                    boxIfRequired(node.value.getInferredType(), target.getInferredType());
                    VarInfo vi = (VarInfo) sym.get(((Identifier) target).name);
                    getVarInfoAddr(A1, vi);
                    backend.emitSW(A0, A1, 0, "Store into local variable");
                }
                if (target instanceof IndexExpr) {
                    handleListIndex((IndexExpr) target, S2);
                }
                if (target instanceof MemberExpr) {
                    handleClassField((MemberExpr) target, S2);
                }
            }
            pop(S2, "Restore S2");
            return null;
        }

        public void allStmts(List<Stmt> stmts) {
            for (Stmt s : stmts) {
                s.dispatch(this);
            }
        }

        @Override
        public Void analyze(WhileStmt node) {
            Label start = generateLocalLabel();
            Label end = generateLocalLabel();

            backend.emitLocalLabel(start, "WHILE:start");
            node.condition.dispatch(this);
            backend.emitBEQZ(A0, end, "WHILE:end");

            allStmts(node.body);
            backend.emitJ(start, "WHILE:start");

            backend.emitLocalLabel(end, "WHILE:end");

            return null;
        }

        public void getVarInfoAddr(RiscVBackend.Register R, VarInfo vi) {
            if (vi instanceof GlobalVarInfo) {
                backend.emitLA(R, ((GlobalVarInfo) vi).getLabel(), String.format("%s: load global address", vi.getVarName()));
            } else {
                getNonGlobalVariableAddr(R, funcInfo, vi);
            }
        }

        private int tempCount = 0;

        @Override
        public Void analyze(ForStmt node) {
            // TODO: binary expression on a function where left side stores S2, right side calls a function with for-loop that returns in the middle
            Label localForLoop = generateLocalLabel();
            Label localForLoopEnd = generateLocalLabel();
            // TODO: Fix this so stmt_for_*_return.py passes
            // Set this up so that you only use the stack instead of S2, S3, S4 like right now.
            // Because control flow gets a bit flunky, you can't pop S2, S3, S4 this way.
            // Set up S2 to be our identifier's address
            push(RA, "Save return address");
            push(S2, " storing S2 in ForStmt");
            SymbolInfo s = sym.get(node.identifier.name);
            if (s instanceof VarInfo) {
                getVarInfoAddr(A0, (VarInfo) s);
            }
            else {
                node.identifier.dispatch(this);
            }
            backend.emitMV(S2, A0, "");

            // Set up S3 to be our iterable
            push(S3, " storing S3 in ForStmt");
            node.iterable.dispatch(this);
            backend.emitMV(S3, A0, "");

            noneCheck(A0, "List should not be None");

            push(S4, " storing S4 in ForStmt");
            // S4 is initially the iterable
            backend.emitMV(S4, S3, "S4 is initially the iterable");
            backend.emitLW(S4, S4, 12, "Now S4 holds the length");
            backend.emitBEQZ(S4, localForLoopEnd, "If len is 0 might as well skip it");

            //tempCount += 3;

            if (node.iterable.getInferredType().equals(Type.STR_TYPE)) {
                // S3 = string
                // S4 = index
//                backend.emitADDI(S3, S3, 16,"S3 = str address + 16 = first char index");
                backend.emitLI(S4, 0, "Store index = 0");
                backend.emitLocalLabel(localForLoop, "Start of for loop");

                backend.emitADDI(T0, S3, 16, "T0 = str (S3) + 16 (start of content)");
                backend.emitADD(T0, T0, S4, "T0 = start of str content (T0) + index (S4)");
                backend.emitLBU(A0, T0, 0, "T0 = character we want");

                backend.emitJAL(boxChar, "Box character into string");
                isBoxChar = true;
                backend.emitSW(A0, S2, 0, "Store A0 (boxchar) into S2 (var)");

                allStmts(node.body);

                backend.emitADDI(S4, S4, 1, "increment index");

                backend.emitLW(T0, S3, 12, "T0 = length of str (S3)");
                backend.emitBGE(S4, T0, localForLoopEnd, "End of loop");

                backend.emitJ(localForLoop, "jump to start");
                backend.emitLocalLabel(localForLoopEnd, "End of for loop");
            } else {
                backend.emitSLLI(S4, S4, 2, "Now S4 denotes the byte offset");
                backend.emitADDI(S3, S3, 16, "Now let S3 holds the first element location");
                backend.emitADD(S4, S3, S4, "Now S4 holds the end of list");

                //
                backend.emitLocalLabel(localForLoop, "Start of for loop");
                // Maps SP(0) to S4
                // Maps SP(-4) to S3
                // Maps SP(
                backend.emitLW(T0, S3, 0, "T0 = deref pointer into list (S3)");
                backend.emitSW(T0, S2, 0, "Store T0 (value) into S2 (location of identifier)");
                allStmts(node.body);
                backend.emitADDI(S3, S3, 4, "");
                backend.emitBNE(S3, S4, localForLoop, "");
                backend.emitLocalLabel(localForLoopEnd, "End of for loop");
            }


            //tempCount -= 3;

            pop(S4, "");
            pop(S3, "");
            pop(S2, "");

            return null;
        }
        @Override
        public Void analyze(IfStmt node) {
            Label then = generateLocalLabel();
            Label end = generateLocalLabel();
            node.condition.dispatch(this);

            backend.emitBNEZ(A0, then, "IF:then");
            allStmts(node.elseBody);
            backend.emitJ(end, "IF:end");

            backend.emitLocalLabel(then, "IF:then");
            allStmts(node.thenBody);

            backend.emitLocalLabel(end, "IF:end");
            return null;
        }

        @Override
        public Void analyze(ReturnStmt node) {
//            for(int i=0;i<tempCount;i++){
//                pop("pop");
//            }
//            tempCount=0;
            if (node.value == null) {
                backend.emitLI(A0, 0, "RETURN:none");
                backend.emitJ(epilogue, "RETURN");
                return null;
            }
            node.value.dispatch(this);
            backend.emitJ(epilogue, "RETURN");
            return null;
        }

        @Override
        public Void analyze(ExprStmt stmt) {
            stmt.expr.dispatch(this);
            return null;
        }

        @Override
        public Void analyze(IntegerLiteral node) {
            backend.emitLI(A0, node.value, String.format("INTEGER: %s", node.value));
            return null;
        }

        @Override
        public Void analyze(BooleanLiteral node) {
            backend.emitLI(A0, node.value ? 1 : 0, String.format("BOOLEAN: %s", node.value));
            return null;
        }

        @Override
        public Void analyze(StringLiteral node) {
            backend.emitLA(A0, constants.getStrConstant(node.value), "STRING");
            return null;
        }

        @Override
        public Void analyze(NoneLiteral node) {
            backend.emitLI(A0, 0, "NONE");
            return null;
        }

        @Override
        public Void analyze(Identifier node) {
            VarInfo vi = (VarInfo) sym.get(node.name);

            getVarInfoAddr(A0, vi);
            backend.emitLW(A0, A0, 0, "Load variable");

            return null;
        }

        @Override
        public Void analyze(UnaryExpr node) {
            node.operand.dispatch(this);

            switch (node.operator) {
                case "-":
                    backend.emitSUB(A0, ZERO, A0, "NEG");
                    break;
                case "not":
                    backend.emitSEQZ(A0, A0, "NOT");
                    break;
            }

            return null;
        }

        @Override
        public Void analyze(BinaryExpr node) {
            new BinaryExprHandler(this, node).handle();
            return null;
        }

        @Override
        public Void analyze(IndexExpr node) {
            if (node.list.getInferredType().equals(Type.STR_TYPE)) {
                node.list.dispatch(this);

                noneCheck(A0, "STRINDEX");

                push(A0, "Push string");

                node.index.dispatch(this);
                backend.emitADDI(A1, A0, 0, "Index in A1");

                pop(A0, "Pop string");

                // A0 = str address
                // A1 = index

                backend.emitLW(A2, A0, 12, "String length");

                oobCheck();

                backend.emitADD(A0, A0, A1, "Offset by index");
                backend.emitLBU(A0, A0, 16, "Load character (@.__str__)");

                backend.emitJAL(boxChar, "boxchar");
                isBoxChar = true;

                return null;
            } else {
                handleListIndex(node, null);
            }
            return null;
        }

        /**
         * Out of bounds check: A1 = index, A2 = length
         */
        private void oobCheck() {
            Label goodLow = generateLocalLabel();

            backend.emitBGEZ(A1, goodLow, "index >= 0 check");
            backend.emitJ(errorOob, "index < 0: oob");
            backend.emitLocalLabel(goodLow, "index >= 0");

            Label goodHigh = generateLocalLabel();

            backend.emitBLT(A1, A2, goodHigh, "index < length check");
            backend.emitJ(errorOob, "index >= length: oob");
            backend.emitLocalLabel(goodHigh, "index < length");
        }

        @Override
        public Void analyze(IfExpr node) {
            node.condition.dispatch(this);
            Label otherwise = generateLocalLabel();
            Label end = generateLocalLabel();
            backend.emitBEQZ(A0, otherwise, "IFEXPR:otherwise");
            node.thenExpr.dispatch(this);
            backend.emitJ(end, "IFEXPR:end");
            backend.emitLocalLabel(otherwise, "IFEXPR:otherwise");
            node.elseExpr.dispatch(this);
            backend.emitLocalLabel(end, "IFEXPR:end");
            return null;
        }

        /**
         * Indexes into a list. If store is null, loads that element into A0. Otherwise, stores that register
         */
        public void handleListIndex(IndexExpr node, RiscVBackend.Register store) {
            node.list.dispatch(this);

            noneCheck(A0, "LIST");

            push(A0, "Push list");


            node.index.dispatch(this);


            // Now list is in A0
            backend.emitMV(A1, A0, "A1 = index");
            pop(A0, "Pop list");

            // Check for length
            // A2 will ultimately be the maximum indexable number through some transformation
            backend.emitADDI(A2, A0, 12, "Location of the length of list on heap");
            backend.emitLW(A2, A2, 0, "Length of this list");

            oobCheck();

            backend.emitSLLI(A1, A1, 2, "4x to get byte offset");
            backend.emitADD(A0, A0, A1, "Offset of list index");

            if (store == null) {
                backend.emitLW(A0, A0, 16, "Load from list");
            } else {
                backend.emitSW(store, A0, 16, "Store into list");
            }
        }
        @Override
        public Void analyze(ListExpr stmt) {
            int numElements = stmt.elements.size();
            backend.emitADDI(SP, SP, -numElements*4 - 4, "Storing elements as well as the len attribute");
            for (int i = 0; i < stmt.elements.size(); i++) {
                stmt.elements.get(i).dispatch(this);
                backend.emitSW(A0, SP, numElements*4 + 4 - (i+1) * 4, "");
            }

            backend.emitLI(T0, numElements, "Load the length into T0");
            backend.emitSW(T0, SP, 0, "");


            backend.emitJAL(boxList, "Jump to list construction");
            isListCons = true;
            backend.emitADDI(SP, SP, numElements*4 + 4, "Releasing elements as well as the length attribute");


            return null;
        }

        private void handleStaticLink(MethodCallExpr node) {
            if (funcInfo == null) return;
            FuncInfo fnInfo = getFuncInfo(node);
            int numArgs = calculateNumArgs(funcInfo);
            if (isRecursive(fnInfo)) {
                backend.emitLW(T0,FP,numArgs*4, "T0=loc spec control link loc");
            }else{
                backend.emitADDI(T0,FP,numArgs*4,"T0=static link of loc of func " + fnInfo.getFuncName());
            }
            push(T0, "pushing static link");
        }

        private void handleStaticLink(CallExpr node) {
            if (funcInfo == null) return;

            FuncInfo fnInfo = getFuncInfo(node);

            int numArgs = calculateNumArgs(funcInfo);

            if (isRecursive(fnInfo)) {
                backend.emitLW(T0, FP, numArgs * 4, "T0 now is at the location specifying the control link location");
            } else {
                // We use fp's indexing instead of sp's indexing,
                // We want to get the parent's static link into the current function,
                // We have already stored ra and control link,
                // We have already restored fp to old sp, which, before invocation, points to either the
                // last argument or the static link
                backend.emitADDI(T0, FP, numArgs * 4,
                        "Store T0 the static link location of function " + fnInfo.getFuncName());
            }
            push(T0, ", storing static link when preparing to invoke a CallExpr on" + fnInfo.getFuncName());
        }

        private FuncInfo getFuncInfo(MethodCallExpr node) {
            ClassInfo c = (ClassInfo) sym.get(node.method.object.getInferredType().className());
            int ind = c.getMethodIndex(node.method.member.name);
            return c.getMethods().get(ind);
        }

        private FuncInfo getFuncInfo(CallExpr node) {
            SymbolInfo s = sym.get(node.function.name);
            return (FuncInfo) s;
        }

        private boolean isRecursive(FuncInfo fnInfo) {
            FuncInfo caller = funcInfo;
            boolean callerIsNull = caller == null;
            boolean isRecursive = !callerIsNull && caller.getFuncName().equals(fnInfo.getFuncName());
            return isRecursive;
        }

        private void popStaticLink(CallExpr node) {
            if (funcInfo == null) return;

            backend.emitADDI(SP, SP, 4, "pop the stackframe corresponding to static link for function");
        }

        private void popStaticLink(MethodCallExpr node) {
            if (funcInfo == null) return;
            backend.emitADDI(SP, SP, 4, "pop static link");
        }

        private void getNonGlobalVariableAddr(RiscVBackend.Register R, FuncInfo funcInfo, VarInfo vi) {
            int numArgs = calculateNumArgs(funcInfo);
            FuncInfo tempFnInfo = funcInfo;

            backend.emitADDI(T0, FP, (numArgs) * 4, "T0 holds the location of static link");
            while (!isLocalOrArg(tempFnInfo, vi) && tempFnInfo.getParentFuncInfo() != null) {
                // We will traverse to the scoping of the parents via static link
                // Figure 2b in impl guide
                // First we need to calculate where the static link is,
                // since during callee execution, fp is at the last parameter, we go back n*4 to find them.

                backend.emitLW(T0, T0, 0, "T0 is loaded the static link");

                tempFnInfo = tempFnInfo.getParentFuncInfo();
            }
            int variableIndex = tempFnInfo.getVarIndex(vi.getVarName()) + 1;

            backend.emitADDI(R,T0, -(variableIndex) * 4, "Get the address");

        }

        private Integer calculateNumArgs(FuncInfo f) {
            if (f == null) return 0;

            return f.getParams().size();
        }


        private boolean isLocalOrArg(FuncInfo funcInfo, VarInfo vi) {
            try {
                funcInfo.getVarIndex(vi.getVarName());
                return true;
            } catch (java.lang.IllegalArgumentException e) {
                return false;
            }
        }

        private void handleFunctionLocals(FuncInfo funcInfo) {
            if (funcInfo == null) return;

            int StackFrameSize = 4 * funcInfo.getLocals().size();
            backend.emitADDI(SP, SP, -StackFrameSize, "Make stack space for local arguments");
            for (int i =0; i < funcInfo.getLocals().size(); i++) {
                funcInfo.getLocals().get(i).getInitialValue().dispatch(this);
                backend.emitSW(A0, SP, StackFrameSize - (i + 1) * 4, "store");
            }
        }

        private void popFunctionLocals(FuncInfo funcInfo) {
            if (funcInfo == null) return;
            int StackFrameSize = 4 * funcInfo.getLocals().size();
            backend.emitADDI(SP, SP, +StackFrameSize, "Retrieve stack space for local arguments");
        }

        private void handleCallExprArgs(CallExpr node) {
            FuncType t = (FuncType) node.function.getInferredType();

            if (node.args.isEmpty()) return;

            int StackFrameSize = 4 * node.args.size();
            backend.emitADDI(SP, SP, -StackFrameSize, "Make stack space for arguments");
            for (int i = 0; i < node.args.size(); i++) {
                node.args.get(i).dispatch(this);
                boxIfRequired(node.args.get(i).getInferredType(), t.parameters.get(i));
                backend.emitSW(A0, SP, StackFrameSize - (i + 1) * 4, "store");
            }
        }

        private void handleCallExprArgs(MethodCallExpr node) {
            FuncType t = (FuncType) node.method.getInferredType();
            int sfs = 4 * (node.args.size() + 1);
            backend.emitADDI(SP, SP, -sfs, "Make stack space for method arguments");

            node.method.object.dispatch(this);

            noneCheck(A0, "Object should not be None");

            backend.emitSW(A0, SP, sfs - 4, "store");
            for(int i = 0; i < node.args.size(); i++) {
                node.args.get(i).dispatch(this);
                boxIfRequired(node.args.get(i).getInferredType(), t.parameters.get(i));
                backend.emitSW(A0, SP, sfs - (i + 2) * 4, "store");
            }
            backend.emitLW(A0, SP, sfs - 4, "Load object back into A0");
        }

        private void popCallExprArgs(CallExpr node) {
            if (node.args.isEmpty()) return;

            backend.emitADDI(SP, SP, 4 * node.args.size(), "Reset stack after function call");
        }

        private void popCallExprArgs(MethodCallExpr node) {
            FuncType t = (FuncType) node.method.getInferredType();
            backend.emitADDI(SP, SP, 4 * (node.args.size() + 1), "Reset stack after method call");
        }


    }

    private void emitListConstruction(Label label) {
        // Set up type tag
        // Set up size: the number of elements in the list + 3
        // Set up displatch table
        // For a class named C, the global symbol for its dispatch table is $C$dispatchTable.

        // Start processing the first attribute at location 12
        // 2nd at location 16
        // n-th at location 8 + 4n

        // total : 8+4n
        backend.emitGlobalLabel(label);

        backend.emitADDI(SP, SP, -8, "Push stack");
        backend.emitSW(RA, SP, 4, "Save return address");
        backend.emitSW(FP, SP, 0, "Save fp");


        backend.emitADDI(FP, SP, 8, "FP is old SP, which is where last arg is");


        // Top value will be the length of the list
        backend.emitLW(A1, FP, 0, "Load the value at top of the stack, this is the length");
        backend.emitLA(A0, listClass.getPrototypeLabel(), "Load the prototype's address to A0");
        // if length is 0 we better jump to the conclusion, which is boxListDone
        // return with us the address of the empty list, which is the prototype
        backend.emitBEQZ(A1, boxListDone, "if length is 0 we better jump to the conclusion, which is boxListDone");
        backend.emitADDI(A1, A1, HeaderSlots, "Add 4 to the size of alloc to box the list");


        // Get the prototype address
        backend.emitJAL(objectAllocResizeLabel, "Jump to alloc2");

        // After this, A0 holds our newly allocated address
        // With (0)A0 holding type tag -1
        // With (4)A0 holding size 4 + n
        // With (8)A0 holding the list's dispatch table
        // We start loading at location (12)A0

        // T0 is our first location in the array, we will employ a branching to load variables
        backend.emitADDI(T0, A0, 16, "");

        // Right now just use T1 to load
        backend.emitLW(T1, FP, 0, "T1 now holds the length");
        backend.emitSW(T1, A0, 12, "Store the length 12 byte from first location in array");
        // T2 holds the number of our list's element
        backend.emitLW(T2, FP, 0, "");
        backend.emitSLLI(T2, T2, 2, "Shift by 2, meaning we mul by 4, meaning we get byte offset");

        // T1 holds the location of our first element on the stack
        backend.emitADD(T1, FP, T2, "T1 holds the location of our first element on the stack");

        // T2 now holds the location of our len
        backend.emitMV(T2, FP, "");

        // is our first location of our argument in the array
        backend.emitGlobalLabel(boxListLoading);
        // Starts loading, so we save
        backend.emitLW(T3, T1, 0, "Load the element from stack");
        backend.emitSW(T3, T0, 0, "Store element to heap");
        backend.emitADDI(T0, T0, 4, "Advance heap by 4");
        backend.emitADDI(T1, T1, -4, "Go down the stack by 4");

        backend.emitBNE(T1,T2, boxListLoading, "If the index are not the same, keep loading" );

        backend.emitGlobalLabel(boxListDone);
        backend.emitLW(FP, SP, 0, "Load fp");
        backend.emitLW(RA, SP, 4, "Load return address");
        backend.emitADDI(SP, SP, 8, "Pop stack");
        backend.emitJR(RA, "Return");
    }

    private void noneCheck(RiscVBackend.Register register, String comment) {
        Label notNone = generateLocalLabel();
        backend.emitBNEZ(register, notNone, comment);
        backend.emitJ(errorNone, null);
        backend.emitLocalLabel(notNone, null);
    }

    /**
     *      Accepts a parameter A0, which is the address of the list, spills the elements onto the stack
     *      Only touches T* parameters in here as well as the stack pointer
      */
    private void listSpillOnStack(RiscVBackend.Register listAddress) {
        Label spillingList = new Label("impl.ListSpill_" + listAddress);
        Label spillingListDone = new Label("impl.ListSpillDone_" + listAddress);

        // T0 is our first location in the array, we will employ a branching to load variables
        backend.emitADDI(T0, listAddress, 16, "T0 is our first location in the array, we will employ a branching to load variables");

        // T2 is the number of our list's element.
        backend.emitLW(T2, listAddress, 12, "T2 is the number of our list's element.");
        backend.emitBEQZ(T2, boxListDone, "if length is 0 we better jump to the conclusion, which is boxListDone");

        backend.emitSLLI(T2, T2, 2, "Get byte offset");
        backend.emitADD(T1, T0, T2, "T1 denotes the end of the list");


        //backend.emitSUB(SP, SP, T2, "Make space on stack to load from heap in listSpillOnStack");
        backend.emitGlobalLabel(spillingList);

        // Begin loading until T0 == T1
        backend.emitLW(T3, T0, 0, "Load element from heap");
        push(T3, " pushing the element of a list from heap onto stack in listSpillOnStack");
        backend.emitADDI(T0, T0, 4 , "Increment by 4");

        backend.emitBNE(T0, T1, spillingList, "Keep on loading");

        backend.emitGlobalLabel(spillingListDone);
    }
    /**
     * Creates a function with `label` that accepts two parameters, A0, and A1, which is
     * the address of the first and second list to concatenate.
     * Also accepts A2, which is the total length
     *
     * This will call the boxList label, with the 2nd parameter which is length
     * @param label
     */
    private void emitListConcat(Label label) {
        backend.emitGlobalLabel(label);

        backend.emitADDI(SP, SP, -8, "Push stack");
        backend.emitSW(RA, SP, 4, "Save return address");
        backend.emitSW(FP, SP, 0, "Save fp");
        backend.emitADDI(FP, SP, 8, "fp is old sp");

        noneCheck(A0, "first list");
        noneCheck(A1, "second list");

        listSpillOnStack(A0);
        listSpillOnStack(A1);
        push(A2, " Finally store the length");
        backend.emitJAL(boxList, " Invoking list creation");
        isListCons = true;
        isListCat = true;
        backend.emitADDI(SP, FP, -8, "Get back SP");
        backend.emitLW(FP, SP, 0, "Load fp");
        backend.emitLW(RA, SP, 4, "Load return address");
        backend.emitADDI(SP, SP, 8, "Pop stack");
        backend.emitJR(RA, "Return");
    }
    protected void emitCustomCode() {
        emitStdFunc(boxInt, "boxint", "chocopy/custom/");
        emitStdFunc(boxBool, "boxbool", "chocopy/custom/");

        if (isBoxChar) {
            emitStdFunc(boxChar, "boxchar", "chocopy/custom/");
        }
        if (isStrCat) {
            emitStdFunc(strcat, "strcat", "chocopy/custom/");
        }
        if (isStrEq) {
            emitStdFunc(streq, "streq", "chocopy/custom/");
        }
        if (isListCat) {
            emitListConcat(listConcatFunc);

        }
        if (isListCons) {
            emitListConstruction(boxList);
        }
        emitErrorFunc(errorNone, "Operation on None", ERROR_NONE);
        emitErrorFunc(errorDiv, "Division by zero", ERROR_DIV_ZERO);
        emitErrorFunc(errorOob, "Index out of bounds", ERROR_OOB);

    }

    private void emitErrorFunc(Label errLabel, String msg, int code) {
        backend.emitGlobalLabel(errLabel);
        backend.emitLI(A0, code, "Exit code for: " + msg);
        backend.emitLA(A1, constants.getStrConstant(msg), "Load error message as str");
        backend.emitADDI(A1, A1, getAttrOffset(strClass, "__str__"), "Load address of attribute __str__");
        backend.emitJ(abortLabel, "Abort");
    }
}
