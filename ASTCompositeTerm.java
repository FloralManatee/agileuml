import java.util.Vector; 
import java.io.*; 

/******************************
* Copyright (c) 2003--2021 Kevin Lano
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0
*
* SPDX-License-Identifier: EPL-2.0
* *****************************/


public class ASTCompositeTerm extends ASTTerm
{ String tag = ""; 
  Vector terms = new Vector(); // of ASTTerm

  public static Expression zeroExpression = 
                             new BasicExpression(0); 
  public static Expression unitExpression = 
                             new BasicExpression(1); 
  public static Expression emptyStringExpression = 
         BasicExpression.newValueBasicExpression("\"\""); 

  public ASTCompositeTerm(String t, Vector subtrees)
  { tag = t; 
    terms = subtrees; 
  } 

  public boolean hasTag(String tagx)
  { return (tagx.equals(tag)); } 

  public String getTag()
  { return tag; } 

  public void addTerm(ASTTerm t) 
  { terms.add(t); } 

  public String toString()
  { String res = "(" + tag; 
    for (int i = 0; i < terms.size(); i++) 
    { res = res + " " + terms.get(i); } 
    res = res + ")"; 
    return res; 
  } 

  public String literalForm()
  { String res = ""; 
    for (int i = 0; i < terms.size(); i++) 
    { ASTTerm t = (ASTTerm) terms.get(i); 
      res = res + t.literalForm(); 
    } 
    return res; 
  } 

  public String asTextModel(PrintWriter out)
  { String id = Identifier.nextIdentifier(tag);
 
    out.println(id + " : " + tag); 
    for (int i = 0; i < terms.size(); i++) 
    { ASTTerm tt = (ASTTerm) terms.get(i); 
      String ttid = tt.asTextModel(out); 
      out.println(id + ".term" + i + " = " + ttid); 
    }
    out.println();  
    return id;  
  } 

  public String cg(CGSpec cgs)
  { // Find the cgs rules r for tag, match the terms to 
    // the r LHS, then apply first rule to the cg results of 
    // the LHS matchings. 

    Vector rules = cgs.getRulesForCategory(tag);
    return cgRules(cgs,rules); 
  } 

  public String cgRules(CGSpec cgs, Vector rules)
  { if (rules == null) 
    { return this + ""; }
 
    for (int i = 0; i < rules.size(); i++) 
    { CGRule r = (CGRule) rules.get(i);
      Vector tokens = r.lhsTokens; 
      Vector vars = r.getVariables(); 

      if (tokens.size() > terms.size())
      { // System.out.println("> " + tag + " rule " + r + " does not match " + this);  
        // System.out.println("!! Too many elements on rule LHS (" + tokens.size() + ") to match subterms: (" + terms.size() + ")"); 
        continue; 
      } 
      else if (vars.contains("_*") && terms.size() >= tokens.size())
      { } // ok 
      else if (tokens.size() == terms.size())
      { } // ok
      else 
      { continue; } 

      // System.out.println("> Trying to match tokens of rule " + r + " for " + this);  
        
      Vector args = new Vector(); 
        // Strings resulting from terms[k].cg(cgs)
      Vector eargs = new Vector(); 
        // the actual terms[k]

      int k = 0; 
      boolean failed = false; 
      for (int j = 0; j < tokens.size() && k < terms.size() && !failed; j++) 
      { String tok = (String) tokens.get(j); 
        ASTTerm tm = (ASTTerm) terms.get(k); 

        if ("_*".equals(tok) && vars.contains(tok))
        { // remainder of terms is processed as a list
          // _* should be the last token, or terminated by 
          // nextTok

          String nextTok = null; 
          if (tokens.size() > j+1)
          { nextTok = (String) tokens.get(j+1); } 

          boolean finished = false; 

          Vector rem = new Vector(); 
          for (int p = j ; p < terms.size() && !finished; p++)
          { ASTTerm pterm = (ASTTerm) terms.get(p); 
            if (nextTok != null && pterm.literalForm().equals(nextTok))
            { finished = true; } 
            else 
            { rem.add(pterm); 
              k++;
            }  
          } 
          eargs.add(rem); 
        } 
        else if (vars.contains(tok))
        { // allocate terms(j) to tok
          eargs.add(tm); 
          k++; 
        } 
        else if (tok.equals(tm.literalForm()))
        { k++; } 
        else 
        { // System.out.println("> " + tag + " rule " + r + " does not match " + this); 
          // System.out.println(tok + " /= " + tm.literalForm()); 
          k++; 
          failed = true; // try next rule 
        } 
      } 

      if (failed == false) 
      { System.out.println("> Matched " + tag + " rule " + r + " for " + this);  

        for (int p = 0; p < eargs.size(); p++)
        { Object obj = eargs.get(p);
          if (obj instanceof ASTTerm) 
          { ASTTerm term = (ASTTerm) obj;  
            String textp = term.cg(cgs); 
            args.add(textp);
          } 
          else if (obj instanceof Vector) 
          { Vector vterms = (Vector) obj; 
            String textp = ""; 
            for (int q = 0; q < vterms.size(); q++) 
            { ASTTerm vterm = (ASTTerm) vterms.get(q); 
              textp = textp + vterm.cg(cgs); 
            } 
            args.add(textp); 
          }     
        } 

        Vector ents = new Vector(); 

        if (r.satisfiesConditions(eargs,ents))
        { System.out.println(">>>> Applying " + tag + " rule " + r); 
          return r.applyRule(args,eargs,cgs); 
        }  
      }
    }  

    System.out.println(); 
    return toString(); 
  }


  public boolean isAssignment()
  { if ("expression".equals(tag) && terms.size() == 3)
    { ASTTerm op = (ASTTerm) terms.get(1);
      if ("=".equals(op))
      { return true; }  
    }
    return false;
  } 

  public String toKM3Assignment()
  { if (terms.size() == 3) // BinaryExpression
    { ASTTerm op = (ASTTerm) terms.get(1); 
      ASTTerm e1 = (ASTTerm) terms.get(0);
      ASTTerm e2 = (ASTTerm) terms.get(2);
      String lhs = e1.toKM3(); 
      String rhs = e2.toKM3(); 
      if (e1.expression != null && e2.expression != null) 
      { statement = new AssignStatement(e1.expression, e2.expression); } 
      return lhs + " := " + rhs; 
    } 
    return toKM3(); 
  } 

  public String toKM3Test()
  { if (tag.equals("forControl") && terms.size() > 2)
    { ASTTerm test = (ASTTerm) terms.get(2);
      
      String res = test.toKM3();
      expression = test.expression; 
      return res;  
    } 
    ASTTerm test = (ASTTerm) terms.get(0);
    String res = test.toKM3();
    expression = test.expression; 
    return res;  
  } 

  public ASTTerm forTest()
  { if (tag.equals("forControl") && terms.size() > 2)
    { ASTTerm test = (ASTTerm) terms.get(2);
      return test; 
    } 
    ASTTerm test = (ASTTerm) terms.get(0);
    return test; 
  } 

  public String toKM3Init()
  { if (tag.equals("forControl"))
    { ASTCompositeTerm init = (ASTCompositeTerm) terms.get(0);
      return init.toKM3Init(); 
    } 
    else if (tag.equals("forInit"))
    { ASTTerm init = (ASTTerm) terms.get(0);
      return init.toKM3(); 
    }  
    return null; 
  } 

  public ASTTerm forInit()
  { if (tag.equals("forControl"))
    { ASTCompositeTerm init = (ASTCompositeTerm) terms.get(0);
      return init.forInit(); 
    } 
    else if (tag.equals("forInit"))
    { ASTTerm init = (ASTTerm) terms.get(0);
      return init; 
    }  
    return null; 
  } 

  public String toKM3Incr()
  { if (tag.equals("forControl") && terms.size() > 4)
    { ASTTerm incr = (ASTTerm) terms.get(4);
      return incr.toKM3();
    } 
    return null;  
  } 

  public ASTTerm forIncr()
  { if (tag.equals("forControl") && terms.size() > 4)
    { ASTTerm incr = (ASTTerm) terms.get(4);
      return incr;
    } 
    return null;  
  } 

  public boolean isEnhancedForControl()
  { if (tag.equals("forControl") && terms.size() > 0)
    { ASTTerm fc = (ASTTerm) terms.get(0);
      if (fc instanceof ASTCompositeTerm && 
          ((ASTCompositeTerm) fc).tag.equals("enhancedForControl"))
      { return true; }
    } 
    return false;  
  } 

  public String toKM3Var()
  { if ("variableDeclarators".equals(tag))
    { ASTCompositeTerm vd1 = (ASTCompositeTerm) terms.get(0);
      return vd1.toKM3Var();
    } // Process each one 

    if ("variableDeclarator".equals(tag))
    { ASTTerm var = (ASTTerm) terms.get(0); 
      return var.toKM3(); 
    } 
    return null;  
  } 

  public String toKM3VarInit()
  { if ("variableDeclarators".equals(tag))
    { ASTCompositeTerm vd1 = (ASTCompositeTerm) terms.get(0);
      String res = vd1.toKM3VarInit();
      // expression = vd1.expression; 
      statement = vd1.statement; 
      return res; 
    } 

    if ("variableDeclarator".equals(tag) && terms.size() > 2)
    { // (variableDeclaratorId x) = (variableInitializer expr)
      
      ASTTerm expr = (ASTTerm) terms.get(2); 
      String res = expr.toKM3();
      // expression = expr.expression;
      statement = expr.statement;  
      return res;  
    } 

    return null;  
  } // and recurse down the list of declarators

  public ASTTerm varInit()
  { if ("variableDeclarators".equals(tag))
    { ASTCompositeTerm vd1 = (ASTCompositeTerm) terms.get(0);
      return vd1.varInit();
    } 

    if ("variableDeclarator".equals(tag) && terms.size() > 2)
    { ASTTerm var = (ASTTerm) terms.get(2); 
      return var; 
    } 

    return null;  
  } // and recurse down the list of declarators

  public String getParNameList(ASTTerm t)
  { Vector pars = getParameterNames(t); 
    String res = ""; 
    for (int i = 0; i < pars.size(); i++) 
    { res = res + pars.get(i); 
      if (i < pars.size() - 1)
      { res = res + ", "; }
    } 
    return res; 
  } 

  public Vector getParameterNames(ASTTerm t) 
  { Vector res = new Vector(); 
    if (t instanceof ASTCompositeTerm) 
    { ASTCompositeTerm tt = (ASTCompositeTerm) t; 
      if ("formalParameters".equals(tt.tag))
      { if (tt.terms.size() > 2)
        { ASTTerm fplist = (ASTTerm) tt.terms.get(1); 
          return getParameterNames(fplist); 
        } 
        return res; // empty parameter list 
      } 

      if ("formalParameterList".equals(tt.tag))
      { for (int i = 0; i < tt.terms.size(); i++) 
        { ASTTerm telem = (ASTTerm) tt.terms.get(i); 
          if (telem instanceof ASTSymbolTerm) { } 
          else 
          { res.add(getParameterName(telem)); } 
        } 
      } 
    } 
    return res; 
  } 

  public String getParameterName(ASTTerm t)
  { String res = ""; 
    if (t instanceof ASTCompositeTerm)
    { ASTCompositeTerm tt = (ASTCompositeTerm) t; 
      if ("formalParameter".equals(tt.tag))
      { int nTerms = tt.terms.size(); 
        ASTTerm varDec = (ASTTerm) tt.terms.get(nTerms - 1); 
        return varDec.toKM3(); 
      } 
    } 
    return res; 
  } 

  public static Vector getExpressions(Vector termList)
  { Vector res = new Vector(); 
    for (int i = 0; i < termList.size(); i++) 
    { ASTTerm trm = (ASTTerm) termList.get(i); 
      if (trm.expression != null) 
      { res.add(trm.expression); } 
    } 
    return res; 
  } 

  public Vector getCallArguments(ASTTerm t) 
  { Vector res = new Vector(); 
    if (t instanceof ASTCompositeTerm) 
    { ASTCompositeTerm tt = (ASTCompositeTerm) t; 
      if ("expressionList".equals(tt.tag))
      { for (int i = 0; i < tt.terms.size(); i++) 
        { ASTTerm telem = (ASTTerm) tt.terms.get(i); 
          if (telem instanceof ASTSymbolTerm) { } 
          else 
          { res.add(telem); } 
        } 
      } 
    } 
    return res; 
  } 

  public String getCallArgumentsTail(ASTTerm t) 
  { String res = ""; 
    if (t instanceof ASTCompositeTerm) 
    { ASTCompositeTerm tt = (ASTCompositeTerm) t; 
      if ("expressionList".equals(tt.tag))
      { for (int i = 2; i < tt.terms.size(); i++) 
        { ASTTerm telem = (ASTTerm) tt.terms.get(i); 
          res = res + telem.toKM3();  
        } 
      } 
    } 
    return res; 
  } 

  public String queryForm()
  { // The return value of an updating operation 
    // such as pop, take, remove
    // Or value returned by a side-effecting expression; 
    // Side-effects are defined by preSideEffect, 
    // postSideEffect

    if ("primary".equals(tag) || "parExpression".equals(tag)) 
    { if (terms.size() == 3)  // ( t ) 
      { ASTTerm tt = (ASTTerm) terms.get(1); 
        String qf = tt.queryForm();
        ASTTerm.setType(this, ASTTerm.getType(tt)); 

        expression = tt.expression; 
        if (expression != null) 
        { expression.setBrackets(true); }   
        return qf; 
      } 

      if (terms.size() == 1) 
      { ASTTerm tt = (ASTTerm) terms.get(0); 
        String qf = tt.queryForm();
        ASTTerm.setType(this, ASTTerm.getType(tt));
        expression = tt.expression; 
        return qf;  
      } 
    } 

    if ("expressionList".equals(tag))
    { Vector strs = new Vector(); 
      Vector exprs = new Vector(); 

      for (int i = 0; i < terms.size(); i++) 
      { ASTTerm telem = (ASTTerm) terms.get(i); 
        if (telem instanceof ASTSymbolTerm) { } 
        else 
        { strs.add(telem.toKM3()); 
          if (telem.expression != null) 
          { exprs.add(telem.expression); }
          statement = telem.statement; // Hack
        }  
      } 
     
      String res = "( "; 
      for (int i = 0; i < strs.size(); i++) 
      { res = res + strs.get(i); 
        if (i < strs.size()-1) 
        { res = res + ","; } 
      }
 
      if (exprs.size() == 1) 
      { expression = (Expression) exprs.get(0); 
        expression.setBrackets(true); 
      }

      return res + " )"; 
    } 
    
    if ("lambdaParameters".equals(tag))
    { if (terms.size() > 1)
      { ASTTerm par = (ASTTerm) terms.get(1); 
        String res = par.queryForm(); 
        expression = par.expression; 
        return res; 
      } 
    } 

    if ("lambdaExpression".equals(tag) && terms.size() > 2)
    { // (lambdaExpression (lambdaParameters ( pars )) -> 
      //    (lambdaBody (block { stats }))
      // Assume 1 variable. 

      ASTTerm pars = (ASTTerm) terms.get(0); 
      String parString = pars.queryForm(); 
      ASTTerm body = (ASTTerm) terms.get(2); 
      String bodyString = body.toKM3(); 
      
      if (body.expression != null) 
      { Type ltype = new Type("OclAny", null);  
        UnaryExpression letexp = 
          UnaryExpression.newUnaryExpression("lambda", body.expression); 
        letexp.accumulator = new Attribute(parString, ltype, ModelElement.INTERNAL); 
        expression = letexp; 
        return letexp + ""; 
      } 

      if (body.statement != null) 
      { Type ltype = new Type("OclAny", null);
        Attribute letvar = new Attribute(parString, ltype, ModelElement.INTERNAL);   
        UnaryExpression letexp = 
          UnaryExpression.newUnaryExpression("lambda", letvar, body.statement, ASTTerm.currentClass); 
        letexp.accumulator = letvar; 
        expression = letexp; 
        return letexp + ""; 
      } 

      return "lambda " + parString + " in " + bodyString; 
    }     

    if ("lambdaBody".equals(tag))
    { if (terms.size() > 1)
      { ASTTerm par = (ASTTerm) terms.get(1); 
        String res = par.queryForm(); 
        expression = par.expression; 
        return res; 
      } 
    } 

    if ("expression".equals(tag))
    { if (terms.size() == 1) 
      { ASTTerm expr = (ASTTerm) terms.get(0); 
        String qf = expr.queryForm();
        ASTTerm.setType(this, ASTTerm.getType(expr));
        expression = expr.expression; 
        return qf; 
      } 

      if (terms.size() == 2) // UnaryExpression
      { ASTTerm op = (ASTTerm) terms.get(0); 
        ASTTerm arg = (ASTTerm) terms.get(1);

        String op1 = op.toKM3(); 
        String arg1 = op.toKM3(); 

        String opx = op.queryForm(); 
        String argx = arg.queryForm(); 


        if ("++".equals(op1))
        { expression = arg.expression; 
          ASTTerm.setType(this, ASTTerm.getType(arg));

          if (arg.expression != null) 
          { Expression unit = new BasicExpression(1); 
            Expression incr = new BinaryExpression("+", arg.expression, unit); 
            statement = new AssignStatement(arg.expression, incr); 
          } 

          System.out.println(">> Query form of " + this + " is: " + expression); 
          System.out.println(">> Update form of " + this + " is: " + statement); 

          return argx; 
        }
 
        if ("--".equals(op1))
        { expression = arg.expression; 
          ASTTerm.setType(this, ASTTerm.getType(arg));

          if (arg.expression != null) 
          { Expression unit = new BasicExpression(1); 
            BinaryExpression incr = new BinaryExpression("-", arg.expression, unit); 
            statement = new AssignStatement(arg.expression, incr); 
          } 

          return argx; 
        }

        if ("++".equals(arg.literalForm()))
        { expression = op.expression; 
          ASTTerm.setType(this, ASTTerm.getType(op));

          if (op.expression != null) 
          { Expression unit = new BasicExpression(1); 
            BinaryExpression incr = new BinaryExpression("+", op.expression, unit); 
            statement = new AssignStatement(op.expression, incr); 
          } 

          System.out.println(">> Query form of " + this + " is: " + expression); 
          System.out.println(">> Update form of " + this + " is: " + statement); 

          return opx; 
        } 

        if ("--".equals(arg.literalForm()))
        { expression = op.expression; 
          ASTTerm.setType(this, ASTTerm.getType(op));

          if (op.expression != null) 
          { Expression unit = new BasicExpression(1); 
            BinaryExpression incr = new BinaryExpression("-", op.expression, unit); 
            statement = new AssignStatement(op.expression, incr); 
          } 

          return opx; 
        } 

        String km3 = opx + argx;

        if (arg.expression != null) 
        { expression = new UnaryExpression(opx, arg.expression); } 

        if (arg.statement != null) 
        { statement = arg.statement; } 


        System.out.println(">> Query form of " + this + " is: " + expression); 
        System.out.println(">> Update form of " + this + " is: " + statement); 
        System.out.println(); 

        return km3; 
      }  

      if (terms.size() > 2 && ".".equals(terms.get(1) + ""))
      { ASTTerm obj = (ASTTerm) terms.get(0); 
        ASTTerm call = (ASTTerm) terms.get(2); 
        return queryForm(obj,call); 
      } // method call query form

      if (terms.size() == 3) // binary expression
      { ASTTerm op = (ASTTerm) terms.get(1); 
        ASTTerm e1 = (ASTTerm) terms.get(0);
        ASTTerm e2 = (ASTTerm) terms.get(2);
        String opx = op.toKM3(); 
        String e1x = e1.queryForm(); 
        String e2x = e2.queryForm();

        if ("&".equals(op + "") && e1.isInteger() && 
            e2.isInteger())
        { ASTTerm.setType(this, "int");
          if (e1.expression != null && 
              e2.expression != null) 
          { Vector parms = new Vector(); 
            parms.add(e1.expression); 
            parms.add(e2.expression); 
            expression = BasicExpression.newStaticCallBasicExpression("bitwiseAnd", "MathLib", parms); 
          } 

          return "MathLib.bitwiseAnd(" + e1x + ", " + e2x + ")"; 
        } 

        if ("|".equals(op + "") && e1.isInteger() && 
            e2.isInteger())
        { ASTTerm.setType(this, "int");

          if (e1.expression != null && 
              e2.expression != null) 
          { Vector parms = new Vector(); 
            parms.add(e1.expression); 
            parms.add(e2.expression); 
            expression = BasicExpression.newStaticCallBasicExpression("bitwiseOr", "MathLib", parms); 
          } 

          return "MathLib.bitwiseOr(" + e1x + ", " + e2x + ")"; 
        } 

        if ("^".equals(op + "") && e1.isInteger() && 
            e2.isInteger())
        { ASTTerm.setType(this, "int");

          if (e1.expression != null && 
              e2.expression != null) 
          { Vector parms = new Vector(); 
            parms.add(e1.expression); 
            parms.add(e2.expression); 
            expression = BasicExpression.newStaticCallBasicExpression("bitwiseXor", "MathLib", parms); 
          } 

          return "MathLib.bitwiseXor(" + e1x + ", " + e2x + ")"; 
        } 

        if (e1.expression != null && e2.expression != null) 
        { expression = new BinaryExpression(opx, e1.expression, e2.expression); }

        return e1x + opx + e2x; 
      } 
        
      if (terms.size() == 4 && 
          "(".equals(terms.get(0) + "") &&
          ")".equals(terms.get(2) + ""))
      { // casting 
        ASTTerm cast = (ASTTerm) terms.get(1); 
        ASTTerm call = (ASTTerm) terms.get(3);

        String ct = cast.toKM3();  
        String qf = call.queryForm(); 

        ASTTerm.setType(this, ct); 

        if ("String".equals(ASTTerm.getType(call)) && 
            ("int".equals(ct) || "long".equals(ct) ||
             "short".equals(ct) || "byte".equals(ct))
           )
        { if (call.expression != null)
          { call.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->char2byte", 
                                  call.expression); 
          }
          return "(" + qf + ")->char2byte()"; 
        }  

        if (call.expression != null && cast.expression != null)
        { call.expression.setBrackets(true); 
          expression = 
            new BinaryExpression("->oclAsType", call.expression, cast.expression); 
        } 

        return "(" + qf + ")->oclAsType(" + ct + ")"; 
      } 

      if (terms.size() == 4 && 
          "[".equals(terms.get(1) + "") && 
          "]".equals(terms.get(3) + "")) // array access
      { ASTTerm arr = (ASTTerm) terms.get(0); 
        ASTTerm ind = (ASTTerm) terms.get(2);

        String elemType = ASTTerm.getElementType(arr); 
        ASTTerm.setType(this, elemType); 

        System.out.println(">>>--->>> element type of " + this + " is " + elemType); 
        System.out.println(); 

        String arrx = arr.queryForm(); 
        String indx = ind.queryForm(); 

        if (arr.expression != null && 
            ind.expression != null)
        { BasicExpression unit = new BasicExpression(1); 
          Expression inde = new BinaryExpression("+", ind.expression, unit);  

          expression = 
             BasicExpression.newIndexedBasicExpression(arr.expression, inde); 
        } 

        return arrx + "[" + indx + " + 1]";
      } 

      if (terms.size() == 5 && "?".equals(terms.get(1) + ""))
      { // ConditionalExpression
        ASTTerm cond = (ASTTerm) terms.get(0); 
        ASTTerm ifoption = (ASTTerm) terms.get(2);
        ASTTerm elseoption = (ASTTerm) terms.get(4);
        String condx = cond.queryForm(); 
        String ifx = ifoption.queryForm(); 
        String elsex = elseoption.queryForm();
        ASTTerm.setType(this, ASTTerm.getType(ifoption)); 
          
        if (cond.expression != null && 
            ifoption.expression != null && 
            elseoption.expression != null) 
        { expression = 
            new ConditionalExpression(cond.expression,
                  ifoption.expression, 
                  elseoption.expression); 
        } 
 
        return "if " + condx + " then " + ifx + " else " + elsex + " endif"; 
      } 

    } 

    return toKM3(); 
  } 

  public String queryForm(ASTTerm arg, ASTTerm call)
  { // arg . call

    String args = arg.toKM3(); 
    String calls = call.toKM3(); 
        
    if (call instanceof ASTCompositeTerm)
    { ASTCompositeTerm callterm = (ASTCompositeTerm) call; 
      if (callterm.tag.equals("methodCall"))
      { Vector callterms = callterm.terms; 
        String called = callterms.get(0) + "";
        ASTTerm callargs = (ASTTerm) callterms.get(2);
        Vector cargs = getCallArguments(callargs); 
          
        if ("pop".equals(called) || "take".equals(called) || 
            "removeLast".equals(called))
        { // _1 = _2.pop(); is 
          // _1 := _2->last() ; _2 := _2->front()

          String elemT = ASTTerm.getElementType(arg); 
          ASTTerm.setType(this, elemT); 

          if (arg.expression != null) 
          { expression = new UnaryExpression("->last", arg.expression); } 

          return args + "->last()";   
        }
        else if ("poll".equals(called))
        { // _1 = _2.pop(); is 
          // _1 := _2->last() ; _2 := _2->front()

          String elemT = ASTTerm.getElementType(arg); 
          ASTTerm.setType(this, elemT); 

          if (arg.expression != null) 
          { expression = new UnaryExpression("->min", arg.expression); } 

          return args + "->min()";   
        }
        else if ("put".equals(called) && arg.isMap())
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          if (arg.expression != null && 
              callarg1.expression != null) 
          { expression = new BinaryExpression("->at", arg.expression, callarg1.expression); } 
           
          return args + "->at(" + callp1 + ")";
        }
        else if ("add".equals(called) && arg.isCollection())
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(this, "boolean"); 

          if (arg.isSet())
          { if (arg.expression != null && 
              callarg1.expression != null) 
            { expression = 
                new BinaryExpression("->excludes",
                      arg.expression, callarg1.expression); 
            } 
            return args + "->excludes(" + callp1 + ")"; 
          } 
          else 
          { expression = new BasicExpression(true); 
            return "true"; 
          } // sequences always change
        }
        else if ("append".equals(called) && arg.isString())
        { String callp = callargs.toKM3(); 

          ASTTerm.setType(this, "String"); 

          if (cargs.size() == 1)
          { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3();
            if (callarg1.isStringSequence())
            { if (arg.expression != null && 
                  callarg1.expression != null) 
              { Expression ue = 
                  new UnaryExpression("->sum", callarg1.expression);  
                expression = new BinaryExpression("+", arg.expression, ue); 
                expression.setBrackets(true); 
              } 
              return "(" + args + " + " + callp1 + "->sum())"; 
            }   

            if (arg.expression != null && 
                callarg1.expression != null) 
            { expression = 
                 new BinaryExpression("+", 
                       arg.expression, callarg1.expression); 
              expression.setBrackets(true); 
            } 

            return "(" + args + " + (\"\" + " + callp1 + "))"; 
          } 
          else if (cargs.size() == 3) 
          { // subrange of first argument
            ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3(); 
            ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 
            ASTTerm callarg3 = (ASTTerm) cargs.get(2);
            String callp3 = callarg3.toKM3();

            if (arg.expression != null && 
                callarg1.expression != null &&  
                callarg2.expression != null &&  
                callarg3.expression != null)
            { BasicExpression unit = new BasicExpression(1);
              Expression sum1 = new BinaryExpression("+", callarg2.expression, unit);  
              Expression sum23 = new BinaryExpression("+", callarg2.expression, callarg3.expression);
              Vector parms = new Vector(); 
              parms.add(sum1); parms.add(sum23); 
              Expression subr = BasicExpression.newFunctionBasicExpression("subrange", callarg1.expression, parms); 
              Expression ue = new UnaryExpression("->sum", subr); 
              expression = new BinaryExpression("+", arg.expression, ue); 
              expression.setBrackets(true); 
            } 
                
            return "(" + args + " + " + callp1 + ".subrange(" + callp2 + "+1, " + callp2 + "+" + callp3 + ")->sum())"; 
          }   
        }
        else if ("insert".equals(called) && arg.isString())
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          String callp = getCallArgumentsTail(callargs); 

          ASTTerm.setType(this, "String"); 

          if (cargs.size() == 2)
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 
            
            if (arg.expression != null && 
                callarg1.expression != null &&  
                callarg2.expression != null)
            { BasicExpression unit = new BasicExpression(1);
              Expression sum1 = new BinaryExpression("+", callarg1.expression, unit);
              Vector parms = new Vector(); 
              parms.add(sum1); 
              parms.add(callarg2.expression); 
              expression = BasicExpression.newFunctionBasicExpression("insertAt", arg.expression, parms); 
            }    
            return args + ".insertAt(" + callp1 + " +1, \"\" + " + callp2 + ")"; 
          } 
          else if (cargs.size() == 4)
          { // subrange of 2nd argument
            ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 
            ASTTerm callarg3 = (ASTTerm) cargs.get(2);
            String callp3 = callarg3.toKM3(); 
            ASTTerm callarg4 = (ASTTerm) cargs.get(3);
            String callp4 = callarg4.toKM3(); 

            if (arg.expression != null && 
                callarg1.expression != null &&  
                callarg2.expression != null && 
                callarg3.expression != null && 
                callarg4.expression != null)
            { BasicExpression unit = new BasicExpression(1);
              Expression sum1 = new BinaryExpression("+", callarg1.expression, unit);

              Vector subpars = new Vector(); 
              subpars.add(
                new BinaryExpression("+", callarg3.expression,
                                     unitExpression)); 
              subpars.add(
                new BinaryExpression("+", callarg3.expression,
                                     callarg4.expression)); 

              Expression arg2subrange = 
                BasicExpression.newFunctionBasicExpression(
                  "subrange", callarg2.expression, subpars); 
              Vector parms = new Vector(); 
              parms.add(sum1); 
              UnaryExpression arg2subrangesum = 
                new UnaryExpression("->sum", arg2subrange); 
              parms.add(arg2subrangesum); 

              expression = BasicExpression.newFunctionBasicExpression("insertAt", arg.expression, parms); 
            }    

            return args + ".insertAt(" + callp1 + "+1, " + callp2 + ".subrange(" + callp3 + "+1, " + callp3 + "+" + callp4 + ")->sum())"; 
          }     
        }
        else if ("replace".equals(called) && arg.isString() && cargs.size() == 3)
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp2 = callarg2.toKM3(); 
          ASTTerm callarg3 = (ASTTerm) cargs.get(2);
          String callp3 = callarg3.toKM3(); 

          ASTTerm.setType(this, "String"); 
          
          return "(" + args + ".subrange(1," + callp1 + ") + " + args + ".subrange(" + callp2 + "+ 1)).insertAt(" + callp1 + " +1, " + callp3 + ")";  
        }
        else if ("addAll".equals(called))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3();

          ASTTerm.setType(this, "boolean");  

          if (arg.isSet())
          { if (arg.expression != null && 
                callarg1.expression != null) 
            { Expression incl = 
                new BinaryExpression("->includesAll", 
                                     arg.expression, 
                                     callarg1.expression); 
              expression = 
                new UnaryExpression("not", incl); 
            } 
            return "not(" + args + "->includesAll(" + callp1 + "))"; 
          } 
          else 
          { expression = new BasicExpression(true); 
            return "true"; 
          } // sequences always change
        }
        else if (( arg.isCollection() || arg.isMap() ) && 
                 ( "remove".equals(called) || 
                   "removeElement".equals(called) )
                )
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          String elemType = ASTTerm.getElementType(arg); 
          ASTTerm.setType(this, elemType);  

          if (callarg1.isInteger())
          { if (arg.expression != null && 
                callarg1.expression != null) 
            { Expression incr = 
                new BinaryExpression("+", callarg1.expression,
                                     unitExpression);  
              expression = 
                new BinaryExpression("->at", 
                                     arg.expression, incr); 
            }  
            return args + "->at(" + callp1 + "+1)"; 
          } 

          if (arg.isMap())
          { if (arg.expression != null && 
                callarg1.expression != null) 
            { expression = 
                new BinaryExpression("->at", 
                      arg.expression, callarg1.expression); 
            }  

            return args + "->at(" + callp1 + ")"; 
          }
 
          if (arg.expression != null && callarg1.expression != null) 
          { expression = new BinaryExpression("->includes", arg.expression, callarg1.expression); }  

          return args + "->includes(" + callp1 + ")"; 
        }
        else if ("removeAll".equals(called))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(this, "boolean"); 

          if (arg.expression != null && 
              callarg1.expression != null) 
          { BinaryExpression be = 
              new BinaryExpression("->intersection", 
                    arg.expression, callarg1.expression); 
            expression = new UnaryExpression("->notEmpty", be); 
          }  

          return args + "->intersection(" + callp1 + ")->notEmpty()"; 
        }
        else if ("retainAll".equals(called))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(this, "boolean"); 

          if (arg.expression != null && callarg1.expression != null) 
          { BinaryExpression be = 
              new BinaryExpression("-", arg.expression, callarg1.expression); 
            be.setBrackets(true); 
            expression = new UnaryExpression("->notEmpty", be); 
          }  

          return "(" + args + " - " + callp1 + ")->notEmpty()"; 
        }
        else if ("removeFirst".equals(called))
        { if (arg.expression != null) 
          { expression = new UnaryExpression("->first", arg.expression); }

          String elemType = ASTTerm.getElementType(arg); 
          ASTTerm.setType(this, elemType); 

          return args + "->first()"; 
        }
        else if ("set".equals(called) && callterms.size() >= 3)
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          String elemType = ASTTerm.getElementType(arg); 
          ASTTerm.setType(this, elemType); 

          if (arg.expression != null && 
              callarg1.expression != null) 
          { expression = new BinaryExpression("->at", 
                arg.expression, callarg1.expression); 
          } 

          return args + "->at(" + callp1 + ")"; 
        }
        else if ("delete".equals(called) && arg.isString())
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(this, "String"); 
                      
          if (cargs.size() > 1) 
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 

            if (arg.expression != null && 
                callarg1.expression != null && 
                callarg2.expression != null) 
            { Expression unit = new BasicExpression(1); 
              Vector pars = new Vector(); 
              pars.add(unit); 
              pars.add(callarg1.expression); 
              Expression sum1 = 
                 BasicExpression.newFunctionBasicExpression("subrange", arg.expression, pars);
              Expression incr = 
                new BinaryExpression("+", callarg2.expression, 
                                     unit);  
              Expression sum2 = 
                 BasicExpression.newFunctionBasicExpression("subrange", arg.expression, incr); 
              expression = 
                new BinaryExpression("+", sum1, sum2); 
            } 

            return args + ".subrange(1," + callp1 + ") + " + 
                   args + ".subrange(" + callp2 + "+1)";
          } 
        } 
        else if ("deleteCharAt".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3();
          ASTTerm.setType(this, "String"); 

          if (arg.expression != null && 
              callarg.expression != null) 
          { Expression unit = new BasicExpression(1); 
            BinaryExpression sum1 = 
              new BinaryExpression("+", callarg.expression, unit); 
            expression = 
              new BinaryExpression("->excludingAt", arg.expression, sum1); 
          } 

          return args + "->excludingAt(" + callp + " + 1)"; 
        } 
        else if ("reverse".equals(called) && arg.isCollection())
        { if (arg.expression != null) 
          { expression = new UnaryExpression("->reverse", arg.expression); }

          ASTTerm.setType(this, ASTTerm.getType(arg)); 
          return args + "->reverse()"; 
        }   
        else if ("replaceAll".equals(called) && "Collections".equals(args) && cargs.size() == 3)
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp2 = callarg2.toKM3(); 
          ASTTerm callarg3 = (ASTTerm) cargs.get(2);
          String callp3 = callarg3.toKM3(); 

          ASTTerm.setType(this, "boolean"); 

          if (callarg1.expression != null && 
              callarg2.expression != null) 
          { expression = 
              new BinaryExpression("->includes", 
                                   callarg1.expression, 
                                   callarg2.expression); 
          } 
          
          return callp1 + "->includes(" + callp2 + ")";  
        }
        else 
        { return featureAccess(arg,call,args,calls); } 
      } // also: removeRange, removeElement
    }

    if (arg.expression != null && call.expression != null &&
        call.expression instanceof BasicExpression) 
    { expression = (BasicExpression) call.expression; 
      ((BasicExpression) expression).setObjectRef(arg.expression); 
    } 

    return args + "." + calls;  
  }  
  



  public String featureAccess(ASTTerm arg, ASTTerm call, String args, String calls)
  { // arg . call
    // args . calls

    String argliteral = arg.literalForm(); 
    String thisliteral = this.literalForm(); 
    Expression argexpr = arg.expression; 
      // assumes already defined

    if (call instanceof ASTCompositeTerm)
    { ASTCompositeTerm callterm = (ASTCompositeTerm) call; 
      if (callterm.tag.equals("methodCall"))
      { Vector callterms = callterm.terms; 
        String called = callterms.get(0) + "";
        System.out.println(">> Method call " + args + "." + calls); 
        
        ASTTerm callargs = (ASTTerm) callterms.get(2);
        Vector cargs = getCallArguments(callargs); 
          
        System.out.println(); 
 
        if ("getRuntime".equals(called) && "Runtime".equals(args))
        { ASTTerm.setType(this, "Runtime"); 
          expression = 
            BasicExpression.newStaticCallBasicExpression(
                                  "getRuntime", "Runtime"); 
          return "Runtime.getRuntime()"; 
        } 
        else if ("exec".equals(called) && 
                 "Runtime".equals(ASTTerm.getType(arg)))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          ASTTerm.setType(thisliteral,"OclProcess");
          if (callarg1.expression != null) 
          { Vector procargs = new Vector(); 
            procargs.add(new BasicExpression("null")); 
            procargs.add(callarg1.expression); 
            expression = 
              BasicExpression.newStaticCallBasicExpression(
                "newOclProcess", "OclProcess", procargs); 
          } 
          return "OclProcess.newOclProcess(null, " + callp1 + ")";  
        }
        else if ("toDegrees".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          ASTTerm.setType(thisliteral,"double"); 
          
          if (callarg1.expression != null) 
          { BasicExpression conver = new BasicExpression(57.29577951308232); 
            expression = 
              new BinaryExpression("*", conver, callarg1.expression); 
            expression.setBrackets(true); 
          } 
          return "(57.29577951308232*(" + callp1 + "))"; 
        }  
        else if ("toRadians".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          ASTTerm.setType(thisliteral,"double"); 
          
          if (callarg1.expression != null) 
          { BasicExpression conver = new BasicExpression(0.017453292519943295); 
            expression = 
              new BinaryExpression("*", conver, callarg1.expression);
            expression.setBrackets(true); 
             
          } 
          return "(0.017453292519943295*(" + callp1 + "))"; 
        }  
        else if ("max".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp1 = callarg1.toKM3(); 
          String callp2 = callarg2.toKM3(); 

          ASTTerm.setType(thisliteral, 
                          ASTTerm.getType(callarg1)); 

          if (callarg1.expression != null && 
              callarg2.expression != null) 
          { SetExpression setexp = new SetExpression();
            setexp.addElement(callarg1.expression); 
            setexp.addElement(callarg2.expression);  
            expression = 
              new UnaryExpression("->max", setexp); 
          } 

          return "Set{" + callp1 + ", " + callp2 + "}->max()"; 
        }  
        else if ("min".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp1 = callarg1.toKM3(); 
          String callp2 = callarg2.toKM3(); 
          ASTTerm.setType(thisliteral, 
                          ASTTerm.getType(callarg1)); 

          if (callarg1.expression != null && 
              callarg2.expression != null) 
          { SetExpression setexp = new SetExpression();
            setexp.addElement(callarg1.expression); 
            setexp.addElement(callarg2.expression);  
            expression = 
              new UnaryExpression("->min", setexp); 
          } 

          return "Set{" + callp1 + ", " + callp2 + "}->min()"; 
        }  
        else if ("abs".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral, 
                          ASTTerm.getType(callarg1)); 

          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->abs",  callarg1.expression); 
          } 
          
          return "(" + callp1 + ")->abs()"; 
        }  
        else if ("abs".equals(called)) // for BigInteger
        { ASTTerm.setType(thisliteral,"long"); 

          if (arg.expression != null) 
          { arg.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->abs", arg.expression); 
          } 

          return "(" + args + ")->abs()"; 
        }  
        else if ("floor".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"int"); 

          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->floor",  callarg1.expression); 
          } 

          return "(" + callp1 + ")->floor()"; 
        }  
        else if ("round".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"int"); 

          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->round",  callarg1.expression); 
          } 
          
          return "(" + callp1 + ")->round()"; 
        }  
        else if ("ceil".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"int"); 
          
          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->ceil",  callarg1.expression); 
          } 

          return "(" + callp1 + ")->ceil()"; 
        }  
        else if ("sin".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"double"); 

          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->sin",  callarg1.expression); 
          } 
          
          return "(" + callp1 + ")->sin()"; 
        }  
        else if ("cos".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->cos",  callarg1.expression); 
          } 

          ASTTerm.setType(thisliteral,"double"); 
          
          return "(" + callp1 + ")->cos()"; 
        }  
        else if ("tan".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->tan",  callarg1.expression); 
          } 

          ASTTerm.setType(thisliteral,"double"); 
          
          return "(" + callp1 + ")->tan()"; 
        }  
        else if ("log".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->log",  callarg1.expression); 
          } 

          ASTTerm.setType(thisliteral,"double"); 
          
          return "(" + callp1 + ")->log()"; 
        }  
        else if ("asin".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->asin",  callarg1.expression); 
          } 

          ASTTerm.setType(thisliteral,"double"); 
          
          return "(" + callp1 + ")->asin()"; 
        }  
        else if ("acos".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"double"); 
          
          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->acos",  callarg1.expression); 
          } 

          return "(" + callp1 + ")->acos()"; 
        }  
        else if ("atan".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->atan",  callarg1.expression); 
          } 

          ASTTerm.setType(thisliteral,"double"); 
          
          return "(" + callp1 + ")->atan()"; 
        }  
        else if ("exp".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->exp",  callarg1.expression); 
          } 

          ASTTerm.setType(thisliteral,"double"); 
          
          return "(" + callp1 + ")->exp()"; 
        }  
        else if ("sinh".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"double"); 
          
          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->sinh",  callarg1.expression); 
          } 

          return "(" + callp1 + ")->sinh()"; 
        }  
        else if ("cosh".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"double"); 
          
          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->cosh",  callarg1.expression); 
          } 

          return "(" + callp1 + ")->cosh()"; 
        }  
        else if ("tanh".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"double"); 
          
          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->tanh",  callarg1.expression); 
          } 

          return "(" + callp1 + ")->tanh()"; 
        }  
        else if ("log10".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"double"); 
          
          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->log10",  callarg1.expression); 
          } 

          return "(" + callp1 + ")->log10()"; 
        }  
        else if ("sqrt".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"double"); 
          
          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->sqrt",  callarg1.expression); 
          } 

          return "(" + callp1 + ")->sqrt()"; 
        }  
        else if ("cbrt".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"double"); 
          
          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->cbrt",  callarg1.expression); 
          } 

          return "(" + callp1 + ")->cbrt()"; 
        }  
        else if ("pow".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp1 = callarg1.toKM3(); 
          String callp2 = callarg2.toKM3(); 

          ASTTerm.setType(thisliteral,"double"); 

          if (callarg1.expression != null && 
              callarg2.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new BinaryExpression("->pow",  callarg1.expression, callarg2.expression); 
          } 

          return "(" + callp1 + ")->pow(" + callp2 + ")"; 
        }  
        else if ("pow".equals(called) && arg.isNumber())
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
         
          ASTTerm.setType(thisliteral,"double"); 

          if (arg.expression != null && 
              callarg1.expression != null) 
          { arg.expression.setBrackets(true); 
            expression = 
              new BinaryExpression("->pow", arg.expression, callarg1.expression); 
          } 

          return "(" + args + ")->pow(" + callp1 + ")"; 
        }  
        else if ("gcd".equals(called) && arg.isNumber())
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          if (arg.expression != null && 
              callarg1.expression != null) 
          { arg.expression.setBrackets(true); 
            expression = 
              new BinaryExpression("->gcd", arg.expression, callarg1.expression); 
          } 
         
          ASTTerm.setType(thisliteral,"long"); 

          return "(" + args + ")->gcd(" + callp1 + ")"; 
        }  
        else if ("rint".equals(called) && "Math".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"double"); 

          if (callarg1.expression != null) 
          { expression = 
              new BinaryExpression("*", new BasicExpression(1.0), 
                new UnaryExpression("->round", callarg1.expression));
            expression.setBrackets(true);  
          } 

          return "(1.0*((" + callp1 + ")->round()))"; 
        }  
        else if ("random".equals(called) && "Math".equals(args))
        { ASTTerm.setType(thisliteral,"double"); 

          expression = BasicExpression.newStaticCallBasicExpression("random", "MathLib", new Vector()); 

          return "MathLib.random()"; 
        }  
        else if ("matches".equals(called) && "Pattern".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1); 
          String callp2 = callarg2.toKM3(); 

          ASTTerm.setType(thisliteral,"boolean"); 

          if (callarg1.expression != null && 
              callarg2.expression != null) 
          { expression = 
                new BinaryExpression("->isMatch", callarg2.expression, callarg1.expression); 
          } 
          
          return callp2 + "->isMatch(" + callp1 + ")"; 
        } 
        else if ("matches".equals(called))
        { ASTTerm.setType(thisliteral,"boolean"); 
          
          if (cargs.size() == 0) // on a Matcher
          { if (arg.expression != null) 
            { expression = 
                 BasicExpression.newCallBasicExpression("isMatch", arg.expression);
            }  
            
            return args + ".isMatch()"; 
          } 
          else if (cargs.size() == 1 && arg.isString())
          { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3(); 
            
            if (callarg1.expression != null && 
                arg.expression != null) 
            { expression = 
                new BinaryExpression("->isMatch", arg.expression, callarg1.expression); 
            } 

            return args + "->isMatch(" + callp1 + ")"; 
          } 
        } 
        else if ("find".equals(called)) // on Matcher
        { if (cargs.size() == 0)
          { ASTTerm.setType(thisliteral,"boolean"); 
          
            if (arg.expression != null) 
            { expression = 
                 BasicExpression.newCallBasicExpression("hasNext", arg.expression);
            }

            return args + ".hasNext()"; 
          } // treat as an OclIterator
        } 
        else if ("list".equals(called) && "Collections".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"Sequence"); 

          if (callarg1.expression != null) 
          { Expression sexpr = new SetExpression(true);
            Expression elems = 
              BasicExpression.newBasicExpression(callarg1.expression, "elements");  
            expression = 
              new BinaryExpression("->union", sexpr, elems); 
          } 

          return "Sequence{}->union(" + callp1 + ".elements)"; 
        } 
        else if ("enumeration".equals(called) && "Collections".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          System.out.println(">>> enumeration of collection: " + this); 

          ASTTerm.setType(thisliteral,"OclIterator"); 

          if (callarg1.expression != null) 
          { expression = 
              BasicExpression.newStaticCallBasicExpression(
                "newOclIterator_Set", "OclIterator", callarg1.expression); 
          } 

          return "OclIterator.newOclIterator_Set(" + callp1 + ")"; 
        }  
        else if ("elements".equals(called) && arg.isMap())
        { 
          System.out.println(">>> enumeration of map values: " + this); 
          
          ASTTerm.setType(thisliteral,"OclIterator"); 

          if (arg.expression != null) 
          { Expression vals = new UnaryExpression("->values", arg.expression); 
            expression = 
              BasicExpression.newStaticCallBasicExpression(
                "newOclIterator_Set", "OclIterator", vals); 
          } 

          return "OclIterator.newOclIterator_Set(" + args + "->values())"; 
        }  
        else if ("min".equals(called) && "Collections".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          String elemT = ASTTerm.getElementType(callarg1); 
          ASTTerm.setType(thisliteral,elemT); 
          
          if (callarg1.expression != null) 
          { expression = new UnaryExpression("->min", callarg1.expression); } 

          return "(" + callp1 + ")->min()"; 
        }  
        else if ("max".equals(called) && "Collections".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          String elemT = ASTTerm.getElementType(callarg1); 
          ASTTerm.setType(thisliteral,elemT); 

          if (callarg1.expression != null) 
          { expression = new UnaryExpression("->max", callarg1.expression); } 
          
          return "(" + callp1 + ")->max()"; 
        } 
        else if ("sort".equals(called) && 
                 ("Collections".equals(args) || 
                  "Arrays".equals(args)))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"Sequence"); 
          
          if (cargs.size() > 2) 
          { ASTTerm lowind = (ASTTerm) cargs.get(1); 
            ASTTerm highind = (ASTTerm) cargs.get(2);
            String lowval = lowind.toKM3(); 
            String highval = highind.toKM3(); 
            return callp1 + " := " + callp1 + ".subrange(1," + lowval + ")^(" + callp1 + ".subrange(" + lowval + "+1," + highval + ")->sort())^" + callp1 + ".subrange(" + highval + "+1," + callp1 + ".size)"; 
          }  

          if (callarg1.expression != null) 
          { expression = new UnaryExpression("->sort", callarg1.expression); 
            statement = 
              new AssignStatement(callarg1.expression,
                                  expression); 
          } 

          return callp1 + " := (" + callp1 + ")->sort()"; 
        } 
        else if ("replaceAll".equals(called) && 
                 "Collections".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,
                          ASTTerm.getType(callarg1)); 
          
          if (cargs.size() > 2) 
          { ASTTerm obj = (ASTTerm) cargs.get(1); 
            ASTTerm rep = (ASTTerm) cargs.get(2);
            String objx = obj.toKM3(); 
            String repx = rep.toKM3();

            if (callarg1.expression != null && 
                obj.expression != null &&  
                rep.expression != null) 
            { Expression xexp = 
                BasicExpression.newVariableBasicExpression("x_1"); 
              Expression eq = 
                new BinaryExpression("=", xexp, obj.expression); 
              Expression conde = 
                new ConditionalExpression(eq,rep.expression,xexp);
              Expression domexpr = 
                new BinaryExpression(":", xexp, callarg1.expression);  
              expression = 
                new BinaryExpression("|C", domexpr, conde); 
              statement = 
                new AssignStatement(callarg1.expression, expression); 
            } 
 
            return callp1 + " := " + callp1 + "->collect(x_1 | if x_1 = " + objx + " then " + repx + " else x_1 endif )"; 
          }  
          return callp1; 
        } 
        else if ("swap".equals(called) && 
                 "Collections".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"Sequence"); 
          
          if (cargs.size() > 2) 
          { ASTTerm obj = (ASTTerm) cargs.get(1); 
            ASTTerm rep = (ASTTerm) cargs.get(2);
            String objx = obj.toKM3(); 
            String repx = rep.toKM3(); 
            return callp1 + " := Integer.subrange(1," + callp1 + ".size)->collect(x_1 | if x_1 = " + objx + "+1 then " + callp1 + "[" + repx + "+1] else if x_1 = " + repx + "+1 then " + callp1 + "[" + objx + "+1] else " + callp1 + "[x_1] endif endif )"; 
          }  
          return callp1; 
        } 
        else if ("rotate".equals(called) && 
                 "Collections".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"Sequence"); 
          
          if (cargs.size() > 1) 
          { ASTTerm nn = (ASTTerm) cargs.get(1); 
            String nx = nn.toKM3(); 
            return callp1 + " := Integer.subrange(1," + callp1 + ".size)->collect( x_1 | " + callp1 + "->at(((x_1-1-" + nx + ") mod " + callp1 + ".size) + 1) )"; 
          }  
          return callp1; 
        } 
        else if ("shuffle".equals(called) && 
                 "Collections".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"Sequence"); 
          
          return callp1 + " := Sequence{}->union(" + callp1 + "->asBag())"; 
        } 
        else if ("forName".equals(called) && "Class".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"OclType"); 

          if (callarg1.expression != null) 
          { Entity ocltypeent = new Entity("OclType"); 
            Type ocltype = new Type(ocltypeent); 
            expression = new BasicExpression(ocltype); 
            expression.setUmlKind(Expression.CLASSID); 
            ((BasicExpression) expression).setArrayIndex(callarg1.expression); 
          } 

          return "OclType[" + callp1 + "]"; 
        } 
        else if ("copyValueOf".equals(called) && "String".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"String"); 
          if (cargs.size() == 1)
          { if (callarg1.expression != null) 
            { callarg1.expression.setBrackets(true); 
              expression = new UnaryExpression("->sum", callarg1.expression); 
            } 
            return "(" + callp1 + ")->sum()"; 
          } 
          else if (cargs.size() == 3)
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1); 
            String callp2 = callarg2.toKM3(); 
            ASTTerm callarg3 = (ASTTerm) cargs.get(2); 
            String callp3 = callarg3.toKM3();

            if (callarg1.expression != null && 
                callarg2.expression != null && 
                callarg3.expression != null) 
            { Vector pars = new Vector(); 
              pars.add(
                new BinaryExpression("+", callarg2.expression,
                                     unitExpression)); 
              pars.add(
                new BinaryExpression("+", callarg2.expression,
                                     callarg3.expression));
              Expression subr = 
                BasicExpression.newFunctionBasicExpression(
                  "subrange", callarg1.expression, pars); 
              expression = 
                new UnaryExpression("->sum", subr); 
            } 
 
            return "(" + callp1 + ").subrange(" + callp2 + "+1, " + callp2 + "+" + callp3 + ")->sum()";  
          }
        } 
        else if ("asList".equals(called) && "Arrays".equals(args))
        { ASTTerm.setType(thisliteral,"Sequence"); 
          ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 

          expression = callarg1.expression; 
          
          return callp1; 
        }
        else if ("binarySearch".equals(called) && "Arrays".equals(args))
        { ASTTerm.setType(thisliteral,"int"); 

          ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp2 = callarg2.toKM3(); 
          
          if (callarg1.expression != null && 
              callarg2.expression != null) 
          { Expression unit = new BasicExpression(1); 
            Expression indof = new BinaryExpression("->indexOf", callarg1.expression, callarg2.expression); 
            expression = new BinaryExpression("-", indof, unit); 
            expression.setBrackets(true); 
          } 

          return "(" + callp1 + "->indexOf(" + callp2 + ") - 1)"; 
        }
        else if ("getByName".equals(called) && 
                 "InetAddress".equals(argliteral))
        { ASTTerm.setType(thisliteral,"String"); 
          ASTTerm callargument1 = (ASTTerm) cargs.get(0);
          String callp1 = callargument1.toKM3(); 

          if (callargument1.expression != null) 
          { expression = callargument1.expression; } 
 
          return callp1; 
        }  
        else if ("valueOf".equals(called) && 
                 "String".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"String"); 

          if (cargs.size() == 1)
          { if (callarg1.expression != null) 
            { Expression emptyString = 
                BasicExpression.newValueBasicExpression("\"\""); 
              expression = new BinaryExpression("+", callarg1.expression, emptyString); 
              expression.setBrackets(true); 
             } 
             return "(" + callp1 + " + \"\")"; 
          } 
          else if (cargs.size() == 3)
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1); 
            String callp2 = callarg2.toKM3(); 
            ASTTerm callarg3 = (ASTTerm) cargs.get(2); 
            String callp3 = callarg3.toKM3();

            if (callarg1.expression != null && 
                callarg2.expression != null && 
                callarg3.expression != null)
            { Vector pars = new Vector(); 
              BasicExpression unit = new BasicExpression(1);
              Expression par1 = 
                new BinaryExpression("+", callarg2.expression,
                                     unit); 
              Expression par2 = 
                new BinaryExpression("+", callarg2.expression,
                                     callarg3.expression); 
              pars.add(par1); pars.add(par2); 
              expression = 
                BasicExpression.newFunctionBasicExpression(
                  "subrange", callarg1.expression, 
                  pars); 
            } 

            return "(" + callp1 + ".subrange(" + callp2 + "+1, " + callp2 + "+" + callp3 + ") + \"\")";  
          }
        } 
        else if ("valueOf".equals(called) && "Boolean".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"boolean"); 
 
          if (callarg1.expression != null) 
          { expression = new UnaryExpression("->toBoolean", callarg1.expression); } 

          return "(" + callp1 + " + \"\")->toBoolean()"; 
        } 
        else if ("valueOf".equals(called) && "BigInteger".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"long"); 

          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = new UnaryExpression("->toLong", callarg1.expression); 
          } 

          return "(" + callp1 + " + \"\")->toLong()"; 
        } 
        else if (("decode".equals(called) || 
                  "valueOf".equals(called) || 
                  "parseByte".equals(called)) && 
                 "Byte".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"int"); 

          if (callarg1.expression != null) 
          { expression = new UnaryExpression("->toInteger", callarg1.expression); } 

          return callp1 + "->toInteger()"; 
        } 
        else if (("decode".equals(called) || 
                  "valueOf".equals(called) || 
                  "parseInt".equals(called)) && 
                 "Integer".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"int"); 

          if (callarg1.expression != null) 
          { expression = new UnaryExpression("->toInteger", callarg1.expression); } 

          return callp1 + "->toInteger()"; 
        } 
        else if (("decode".equals(called) || 
                  "valueOf".equals(called) || 
                  "parseLong".equals(called)) && 
                 "Long".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"long"); 

          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = new UnaryExpression("->toLong", callarg1.expression); 
          } 

          return "(" + callp1 + ")->toLong()"; 
        } 
        else if (("valueOf".equals(called) || 
                  "parseDouble".equals(called)) && 
                 "Double".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"double"); 

          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->toReal", callarg1.expression); 
          } 

          return "(" + callp1 + " + \"\")->toReal()"; 
        } 
        else if (("valueOf".equals(called) || 
                  "parseFloat".equals(called)) && 
                 "Float".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"double"); 

          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = new UnaryExpression("->toReal", callarg1.expression); 
          } 

          return "(" + callp1 + " + \"\")->toReal()"; 
        } 
        else if (("decode".equals(called) || 
                  "valueOf".equals(called) || 
                  "parseShort".equals(called)) && 
                 "Short".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"int"); 

          if (callarg1.expression != null) 
          { callarg1.expression.setBrackets(true); 
            expression = new UnaryExpression("->toInteger", callarg1.expression); 
          } 

          return "(" + callp1 + ")->toInteger()"; 
        } 
        else if ("toBinaryString".equals(called) &&  
                 ("Integer".equals(argliteral) || 
                  "Long".equals(argliteral)))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"String"); 

          if (callarg1.expression != null) 
          { Vector parms = new Vector(); 
            parms.add(callarg1.expression); 
             
            expression = BasicExpression.newStaticCallBasicExpression("decimal2binary", "MathLib", parms); 
          } 

          return "MathLib.decimal2binary(" + callp1 + ")"; 
        } 
        else if ("toHexString".equals(called) &&  
                 ("Integer".equals(argliteral) || 
                  "Long".equals(argliteral)))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"String"); 

          if (callarg1.expression != null) 
          { Vector parms = new Vector(); 
            parms.add(callarg1.expression); 
             
            expression = BasicExpression.newStaticCallBasicExpression("decimal2hex", "MathLib", parms); 
          }

          return "MathLib.decimal2hex(" + callp1 + ")"; 
        } 
        else if ("toOctalString".equals(called) &&  
                 ("Integer".equals(argliteral) || 
                  "Long".equals(argliteral)))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"String"); 

          if (callarg1.expression != null) 
          { Vector parms = new Vector(); 
            parms.add(callarg1.expression); 
             
            expression = BasicExpression.newStaticCallBasicExpression("decimal2octal", "MathLib", parms); 
          }

          return "MathLib.decimal2octal(" + callp1 + ")"; 
        } 
        else if ("toString".equals(called) && 
                 ("Byte".equals(argliteral) ||
                  "Float".equals(argliteral) || 
                  "Double".equals(argliteral) || 
                  "Integer".equals(argliteral) || 
                  "Long".equals(argliteral) || 
                  "Short".equals(argliteral)) ) 
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"String"); 

          if (callarg1.expression != null) 
          { Expression emptyString = BasicExpression.newValueBasicExpression("\"\""); 
            emptyString.setType(new Type("String", null)); 
            expression = new BinaryExpression("+", callarg1.expression, emptyString); 
            expression.setBrackets(true); 
          } 

          return "(" + callp1 + " + \"\")"; 
        } 
        else if ("isNaN".equals(called) && 
                 ("Double".equals(argliteral) || 
                  "Float".equals(argliteral)) )
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"boolean"); 

          if (callarg1.expression != null) 
          { expression = new UnaryExpression("->oclIsInvalid", callarg1.expression); } 

          return "(" + callp1 + "->oclIsInvalid())"; 
        } 
        else if ("isInfinite".equals(called) && 
                 ("Double".equals(argliteral) || 
                  "Float".equals(argliteral)) )
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"boolean"); 

          if (arg.expression != null) 
          { Expression pinf = 
              BasicExpression.newValueBasicExpression("Math_PINFINITY"); 
            Expression eqPInf = 
              new BinaryExpression("=", arg.expression, pinf); 
            eqPInf.setBrackets(true); 
            Expression ninf = 
              BasicExpression.newValueBasicExpression("Math_NINFINITY"); 
            Expression eqNInf = 
              new BinaryExpression("=", arg.expression, ninf); 
            eqNInf.setBrackets(true); 

            expression = new BinaryExpression("or", eqPInf, eqNInf); 
            expression.setBrackets(true); 
          } 

          return "((" + args + " = Math_PINFINITY) or (" + args + " = Math_NINFINITY))"; 
        } 
        else if ("isNaN".equals(called))
        { 
          ASTTerm.setType(thisliteral,"boolean"); 

          if (arg.expression != null) 
          { expression = new UnaryExpression("->oclIsInvalid", arg.expression); } 

          return "(" + args + "->oclIsInvalid())"; 
        } 
        else if ("isInfinite".equals(called))
        { 
          ASTTerm.setType(thisliteral,"boolean"); 

          if (arg.expression != null) 
          { Expression pinf = 
              BasicExpression.newValueBasicExpression("Math_PINFINITY"); 
            Expression eqPInf = 
              new BinaryExpression("=", arg.expression, pinf); 
            eqPInf.setBrackets(true); 
            Expression ninf = 
              BasicExpression.newValueBasicExpression("Math_NINFINITY"); 
            Expression eqNInf = 
              new BinaryExpression("=", arg.expression, ninf); 
            eqNInf.setBrackets(true); 

            expression = new BinaryExpression("or", eqPInf, eqNInf); 
            expression.setBrackets(true); 
          } 

          return "((" + args + " = Math_PINFINITY) or (" + args + " = Math_NINFINITY))"; 
        } 
        else if ("compare".equals(called) && 
                 ("Double".equals(argliteral) || 
                  "Float".equals(argliteral)) )
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1); 
          String callp2 = callarg2.toKM3(); 
          
          ASTTerm.setType(thisliteral,"int"); 

          if (callarg1.expression != null && 
              callarg2.expression != null) 
          { expression = new BinaryExpression("->compareTo", callarg1.expression, callarg2.expression); } 

          return callp1 + "->compareTo(" + callp2 + ")"; 
        } 
        else if ("reverse".equals(called) && "Collections".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,
                          ASTTerm.getType(arg)); 
          
          if (callarg1.expression != null) 
          { expression = new UnaryExpression("->reverse", callarg1.expression); 
            statement = new AssignStatement(callarg1.expression, expression); 
          }

          return callp1 + " := (" + callp1 + ")->reverse()"; 
        } 
        else if ("reverse".equals(called) && arg.isString())
        { 
          ASTTerm.setType(thisliteral,"String"); 
          
          if (arg.expression != null) 
          { expression = new UnaryExpression("->reverse", arg.expression); 
            statement = new AssignStatement(arg.expression, expression); 
          }

          return args + " := (" + args + ")->reverse()"; 
        } 
        else if ("copy".equals(called) && "Collections".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1); 
          String callp2 = callarg2.toKM3();

          ASTTerm.setType(thisliteral,
                          ASTTerm.getType(arg)); 
          
          if (callarg1.expression != null && 
              callarg2.expression != null) 
          { expression = new UnaryExpression("->copy", callarg2.expression); 
            statement = new AssignStatement(callarg1.expression, expression); 
          }

          return callp1 + " := Sequence{}->union(" + callp2 + ")"; 
        } 
        else if ("fill".equals(called) && 
                 ("Collections".equals(args) || 
                  "Arrays".equals(args)))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"Sequence"); 
          
          if (cargs.size() == 2)
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1); 
            String callp2 = callarg2.toKM3();

            if (callarg1.expression != null && 
                callarg2.expression != null) 
            { expression = 
                new BinaryExpression("->collect", 
                                   callarg1.expression, 
                                   callarg2.expression); 
              statement = 
                new AssignStatement(callarg1.expression, 
                                    expression); 
            } 

            return callp1 + " := (" + callp1 + ")->collect(" + callp2 + ")"; 
          } 
          else if (cargs.size() > 2) 
          { ASTTerm lowind = (ASTTerm) cargs.get(1); 
            ASTTerm highind = (ASTTerm) cargs.get(2);
            ASTTerm elem = (ASTTerm) cargs.get(3);
            String lowval = lowind.toKM3(); 
            String highval = highind.toKM3(); 
            String val = elem.toKM3(); 

            return callp1 + " := " + callp1 + ".subrange(1," + lowval + ")^(" + callp1 + ".subrange(" + lowval + "+1," + highval + ")->collect(" + val + "))^" + callp1 + ".subrange(" + highval + "+1," + callp1 + ".size)"; 
          }  
        } 
        else if ("nCopies".equals(called) && "Collections".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp1 = callarg1.toKM3(); 
          String callp2 = callarg2.toKM3(); 

          String elemT = ASTTerm.getType(callarg2); 
          if (elemT != null) 
          { ASTTerm.setType(thisliteral,
                            "Sequence(" + elemT + ")");
          } 
          else
          { ASTTerm.setType(thisliteral,"Sequence"); }

          if (callarg1.expression != null && 
              callarg2.expression != null) 
          { Vector pars = new Vector(); 
            pars.add(new BasicExpression(1)); 
            pars.add(callarg1.expression); 
            Expression subrang = BasicExpression.newFunctionBasicExpression("subrange", 
                "Integer", pars); 
            expression = new BinaryExpression("->collect", 
                subrang, callarg2.expression); 
          } 

          return "Integer.subrange(1," + callp1 + ")->collect(" + callp2 + ")"; 
        }  
        else if ("binarySearch".equals(called) && "Collections".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp1 = callarg1.toKM3(); 
          String callp2 = callarg2.toKM3(); 

          ASTTerm.setType(thisliteral,"int"); 

          if (callarg1.expression != null && 
              callarg2.expression != null) 
          { Expression unit = new BasicExpression(1); 
            Expression indof = new BinaryExpression("->indexOf", callarg1.expression, callarg2.expression); 
            expression = new BinaryExpression("-", indof, unit); 
            expression.setBrackets(true); 
          } 

          return "((" + callp1 + ")->indexOf(" + callp2 + ")-1)"; 
        } 
        else if ("search".equals(called) && arg.isSequence())
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"int"); 

          if (arg.expression != null && 
              callarg1.expression != null) 
          { Expression unit = new BasicExpression(1);
            Expression incl = 
              new BinaryExpression("->includes", 
                    arg.expression, callarg1.expression);
            Expression argsze = 
              new UnaryExpression("->size", arg.expression);  
            Expression argind = 
              new BinaryExpression("->indexOf", arg.expression, callarg1.expression); 
            Expression szexpr = new BinaryExpression("-", argsze, argind); 
            szexpr = new BinaryExpression("+", szexpr, unit);

            expression = new ConditionalExpression(incl,
                               szexpr, 
                               new BasicExpression(-1)); 
          } 

          return "if (" + args + ")->includes(" + callp1 + ") then " + args + "->size() - " + args + "->indexOf(" + callp1 + ") + 1 else -1 endif"; 
        } 
        else if ("indexOfSubList".equals(called) && "Collections".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp1 = callarg1.toKM3(); 
          String callp2 = callarg2.toKM3(); 

          ASTTerm.setType(thisliteral,"int"); 

          if (callarg1.expression != null && 
              callarg2.expression != null) 
          { Expression unit = new BasicExpression(1); 
            Expression indof = new BinaryExpression("->indexOf", callarg1.expression, callarg2.expression); 
            expression = new BinaryExpression("-", indof, unit); 
            expression.setBrackets(true); 
          } 

          return "((" + callp1 + ")->indexOf(" + callp2 + ")-1)"; 
        }  
        else if ("lastIndexOfSubList".equals(called) && "Collections".equals(args))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp1 = callarg1.toKM3(); 
          String callp2 = callarg2.toKM3(); 

          ASTTerm.setType(thisliteral,"int"); 

          if (callarg1.expression != null && 
              callarg2.expression != null) 
          { Expression unit = new BasicExpression(1); 
            Expression indof = new BinaryExpression("->lastIndexOf", callarg1.expression, callarg2.expression); 
            expression = new BinaryExpression("-", indof, unit); 
            expression.setBrackets(true); 
          } 

          return "((" + callp1 + ")->lastIndexOf(" + callp2 + ")-1)"; 
        }  
        else if ("Collections".equals(args) && 
                 isCollectionsWrapperOp(called))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(this,ASTTerm.getType(callarg1)); 
     
          expression = callarg1.expression; 
    
          return callp1; 
        }  
        else if ("Collections".equals(args) && 
                 "singletonList".equals(called))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          String elemT = ASTTerm.getType(callarg1); 
          if (elemT != null) 
          { ASTTerm.setType(this, "Sequence(" + elemT + ")"); } 
          else
          { ASTTerm.setType(this,"Sequence"); }

          if (callarg1.expression != null) 
          { expression = new SetExpression(true); 
            ((SetExpression) expression).addElement(callarg1.expression); 
          }  
         
          return "Sequence{" + callp1 + "}"; 
        }  
        else if ("Collections".equals(args) && 
                 "singleton".equals(called))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          String elemT = ASTTerm.getType(callarg1); 
          if (elemT != null) 
          { ASTTerm.setType(this, "Set(" + elemT + ")"); } 
          else
          { ASTTerm.setType(this,"Set"); } 

          if (callarg1.expression != null) 
          { expression = new SetExpression(); 
            ((SetExpression) expression).addElement(callarg1.expression); 
          }  
         
          return "Set{" + callp1 + "}"; 
        }  
        else if ("Collections".equals(args) && 
                 "singletonMap".equals(called))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1); 
          String callp2 = callarg2.toKM3(); 
         
          ASTTerm.setType(this,"Map");

          if (callarg1.expression != null && 
              callarg2.expression != null) 
          { expression = new SetExpression();
            expression.setType(new Type("Map", null));  
            ((SetExpression) expression).addMapElement(callarg1.expression, 
                                     callarg2.expression); 
          }  

          return "Map{ " + callp1 + " |-> " + callp2 + " }"; 
        }  
        else if ("toUpperCase".equals(called) && "Character".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"String"); 

          if (callarg1.expression != null) 
          { expression = new UnaryExpression("->toUpperCase", callarg1.expression); } 

          return "(" + callp1 + " + \"\")->toUpperCase()"; 
        }  
        else if ("toLowerCase".equals(called) && "Character".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"String"); 
          if (callarg1.expression != null) 
          { expression = new UnaryExpression("->toLowerCase", callarg1.expression); } 

          return "(" + callp1 + " + \"\")->toLowerCase()"; 
        }  
        else if ("isDigit".equals(called) && "Character".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"boolean"); 

          if (callarg1.expression != null) 
          { Expression digitString = 
              BasicExpression.newValueBasicExpression(
                                          "\"[0-9]\""); 
            expression = 
              new BinaryExpression("->isMatch", 
                    callarg1.expression, digitString); 
          } 

          return "(" + callp1 + " + \"\")->isMatch(\"[0-9]\")"; 
        }  
        else if ("isLetter".equals(called) && "Character".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"boolean"); 

          if (callarg1.expression != null) 
          { Expression letterString = 
              BasicExpression.newValueBasicExpression(
                                          "\"[a-zA-Z]\""); 
            expression = 
              new BinaryExpression("->isMatch", 
                    callarg1.expression, letterString); 
          } 
          
          return "(" + callp1 + " + \"\")->isMatch(\"[a-zA-Z]\")"; 
        }  
        else if ("isLowerCase".equals(called) && "Character".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"boolean"); 

          if (callarg1.expression != null) 
          { Expression lcExpr = 
              new UnaryExpression("->toLowerCase",
                                  callarg1.expression); 
            expression = 
              new BinaryExpression("=", lcExpr, 
                                   callarg1.expression); 
            expression.setBrackets(true); 
          } 

          return "(" + callp1 + "->toLowerCase() = " + callp1 + ")"; 
        }  
        else if ("isUpperCase".equals(called) && "Character".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
                    
          ASTTerm.setType(thisliteral,"boolean"); 

          if (callarg1.expression != null) 
          { Expression ucExpr = 
              new UnaryExpression("->toUpperCase",
                                  callarg1.expression); 
            expression = 
              new BinaryExpression("=", ucExpr, 
                                   callarg1.expression);
            expression.setBrackets(true);  
          } 

          return "(" + callp1 + "->toUpperCase() = " + callp1 + ")"; 
        }  
        else if ("isLetterOrDigit".equals(called) && "Character".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"boolean"); 

          if (callarg1.expression != null) 
          { Expression letterDigitString = 
              BasicExpression.newValueBasicExpression(
                                          "\"[a-zA-Z0-9]\""); 
            expression = 
              new BinaryExpression("->isMatch", 
                    callarg1.expression, letterDigitString); 
          } 
          
          return "(" + callp1 + " + \"\")->isMatch(\"[a-zA-Z0-9]\")"; 
        }
        else if ("isJavaIdentifierPart".equals(called) && "Character".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"boolean"); 

          if (callarg1.expression != null) 
          { Expression jpString = 
              BasicExpression.newValueBasicExpression(
                                    "\"[0-9a-zA-Z_$]\""); 
            expression = 
              new BinaryExpression("->isMatch", 
                    callarg1.expression, jpString); 
          } 
          
          return "(" + callp1 + " + \"\")->isMatch(\"[0-9a-zA-Z_$]\")"; 
        }    
        else if ("isJavaIdentifierStart".equals(called) && "Character".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"boolean"); 

          if (callarg1.expression != null) 
          { Expression jsString = 
              BasicExpression.newValueBasicExpression(
                                    "\"[a-zA-Z_$]\""); 
            expression = 
              new BinaryExpression("->isMatch", 
                    callarg1.expression, jsString); 
          } 

          return "(" + callp1 + " + \"\")->isMatch(\"[a-zA-Z_$]\")"; 
        }    
        else if ("isSpaceChar".equals(called) && "Character".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"boolean"); 
          
          if (callarg1.expression != null) 
          { Expression spaceString = 
              BasicExpression.newValueBasicExpression(
                                    "\"[ \\t\\n]\""); 
            expression = 
              new BinaryExpression("->isMatch", 
                    callarg1.expression, spaceString); 
          } 

          return "(" + callp1 + " + \"\")->isMatch(\"[ \\t\\n]\")"; 
        }    
        else if ("isWhitespace".equals(called) && "Character".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"boolean"); 

          if (callarg1.expression != null) 
          { Expression spaceString = 
              BasicExpression.newValueBasicExpression(
                                    "\"[ \\t\\n]\""); 
            expression = 
              new BinaryExpression("->isMatch", 
                    callarg1.expression, spaceString); 
          } 

          return "(" + callp1 + " + \"\")->isMatch(\"[ \\t\\n]\")"; 
        }    
        else if ("toString".equals(called) && "Character".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(thisliteral,"String"); 

          if (callarg1.expression != null) 
          { Expression spaceString = 
              BasicExpression.newValueBasicExpression(
                                    "\"\""); 
            expression = 
              new BinaryExpression("+", 
                    callarg1.expression, spaceString);
            expression.setBrackets(true);  
          } 

          return "(" + callp1 + " + \"\")"; 
        }  
        else if (("digit".equals(called) || 
                  "getNumericValue".equals(called)) && "Character".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3();

          ASTTerm.setType(thisliteral,"int"); 

          if (callarg1.expression != null) 
          { Expression spaceString = 
              BasicExpression.newValueBasicExpression(
                                    "\"\""); 
            Expression ux = 
              new BinaryExpression("+", 
                    callarg1.expression, spaceString);
            ux.setBrackets(true);
            expression = 
              new UnaryExpression("->toInteger", ux);   
          } 
          
          return "(" + callp1 + " + \"\")->toInteger()"; 
        }  
        else if ("forDigit".equals(called) && "Character".equals(argliteral))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          
          ASTTerm.setType(thisliteral,"String"); 

          if (callarg1.expression != null) 
          { Expression spaceString = 
              BasicExpression.newValueBasicExpression(
                                    "\"\""); 
            expression = 
              new BinaryExpression("+", 
                    callarg1.expression, spaceString);
            expression.setBrackets(true);  
          } 

          return "(" + callp1 + " + \"\")"; 
        }  
        else if ("getClass".equals(called))
        { ASTTerm.setType(thisliteral,"OclType"); 

          if (arg.expression != null) 
          { expression = new UnaryExpression("->oclType", arg.expression); } 

          return args + "->oclType()"; 
        } 
        else if ("finalize".equals(called))
        { if (arg.expression != null) 
          { expression = 
              new UnaryExpression("->isDeleted", 
                                  arg.expression); 
            statement = 
              new ImplicitInvocationStatement(expression); 
          } 

          return "    execute " + args + "->isDeleted()"; 
        } 
        else if ("notify".equals(called))
        { if (arg.expression != null) 
          { expression = 
              BasicExpression.newStaticCallBasicExpression(
                "notify", "OclProcess", arg.expression); 
            statement = 
              InvocationStatement.newInvocationStatement(
                expression, arg.expression); 
          } 
          return "    OclProcess.notify(" + args + ")"; 
        } 
        else if ("notifyAll".equals(called))
        { if (arg.expression != null) 
          { expression = 
              BasicExpression.newStaticCallBasicExpression(
                "notifyAll", "OclProcess", arg.expression); 
            statement = 
              InvocationStatement.newInvocationStatement(
                expression, arg.expression); 
          }
          return "    OclProcess.notifyAll(" + args + ")"; 
        } 
        else if ("wait".equals(called))
        { if (cargs.size() == 0)
          { if (arg.expression != null) 
            { Vector pars = new Vector(); 
              pars.add(arg.expression); 
              pars.add(new BasicExpression(0)); 
              expression = 
                BasicExpression.newStaticCallBasicExpression(
                            "wait", "OclProcess", pars); 
              statement = 
                InvocationStatement.newInvocationStatement(
                  expression, pars); 
            }
            return "    OclProcess.wait(" + args + ",0)"; 
          } 
          else if (cargs.size() == 1)
          { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3(); 
            if (arg.expression != null && 
                callarg1.expression != null) 
            { Vector pars = new Vector(); 
              pars.add(arg.expression); 
              pars.add(callarg1.expression); 
              expression = 
                BasicExpression.newStaticCallBasicExpression(
                            "wait", "OclProcess", pars); 
              statement = 
                InvocationStatement.newInvocationStatement(
                  expression, pars); 
            }
            return "    OclProcess.wait(" + args + "," + callp1 + ")"; 
          } 
          else 
          { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3(); 
            ASTTerm callarg2 = (ASTTerm) cargs.get(1); 
            String callp2 = callarg2.toKM3();

            if (arg.expression != null && 
                callarg1.expression != null && 
                callarg2.expression != null) 
            { Vector pars = new Vector(); 
              pars.add(arg.expression); 
              Expression div = new BasicExpression(1000000.0);
              Expression arg2div = 
                new BinaryExpression("/", callarg2.expression, div); 
              Expression par2 = 
                new BinaryExpression("+", callarg1.expression, arg2div); 
              pars.add(par2);  
              expression = 
                BasicExpression.newStaticCallBasicExpression(
                            "wait", "OclProcess", pars); 
              statement = 
                InvocationStatement.newInvocationStatement(
                  expression, pars); 
            } 
            return "    OclProcess.wait(" + args + "," + callp1 + " + " + callp2 + "/1000000.0)"; 
          }  
        } 
        else if ("Thread".equals(args) && 
                 "enumerate".equals(called))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3();

          if (callarg1.expression != null) 
          { expression = 
              BasicExpression.newStaticCallBasicExpression(
                "allActiveThreads", "OclProcess"); 
            statement = 
              new AssignStatement(callarg1.expression, 
                                  expression); 
          } 
 
          return "    " + callp1 + " := OclProcess.allActiveThreads()"; 
        }   
        else if (arg.isProcess() && 
                 "join".equals(called))
        { if (cargs.size() == 0)
          { if (arg.expression != null) 
            { Vector pars = new Vector(); 
              pars.add(new BasicExpression(0)); 
              expression = 
                BasicExpression.newCallBasicExpression(
                  "join", arg.expression, pars); 
              statement = 
                InvocationStatement.newInvocationStatement(
                                            expression,pars); 
            } 
 
            return "    " + arg + ".join(0)";
          }  
        }   
        else if ("clone".equals(called))
        { if (arg.expression != null) 
          { expression = new UnaryExpression("->copy", arg.expression); } 
          ASTTerm.setType(this, ASTTerm.getType(arg)); 

          return args + "->copy()"; 
        } 
        else if ("doubleValue".equals(called) || "floatValue".equals(called))
        { ASTTerm.setType(thisliteral,"double"); 

          if (arg.expression != null) 
          { expression = new UnaryExpression("->toReal", arg.expression); } 

          return "(" + args + " + \"\")->toReal()"; 
        } 
        else if ("booleanValue".equals(called))
        { ASTTerm.setType(thisliteral,"boolean"); 

          if (arg.expression != null) 
          { expression = new UnaryExpression("->toBoolean", arg.expression); } 

          return "(" + args + " + \"\")->toBoolean()"; 
        } 
        else if ("charValue".equals(called))
        { ASTTerm.setType(thisliteral,"String"); 

          if (arg.expression != null) 
          { Expression spaceString = 
              BasicExpression.newValueBasicExpression(
                                    "\"\""); 
            expression = 
              new BinaryExpression("+", 
                    arg.expression, spaceString);
            expression.setBrackets(true);  
          } 

          return "(" + args + " + \"\")"; 
        } 
        else if ("longValue".equals(called))
        { ASTTerm.setType(thisliteral,"long"); 

          if (arg.expression != null) 
          { expression = new UnaryExpression("->toLong", arg.expression); } 

          return "(" + args + " + \"\")->toLong()"; 
        } 
        else if ("byteValue".equals(called) ||
                 "intValue".equals(called) || 
                 "shortValue".equals(called))
        { ASTTerm.setType(thisliteral,"int"); 

          if (arg.expression != null) 
          { expression = new UnaryExpression("->toInteger", arg.expression); } 

          return "(" + args + " + \"\")->toInteger()"; 
        } 
        else if ("hasNext".equals(called) || "hasMoreElements".equals(called) || "hasMoreTokens".equals(called))
        { String tt = ASTTerm.getType(args); 
          System.out.println(">>> Type of " + args + " is: " + tt);  
           
          if ("OclIterator".equals(tt)) 
          { ASTTerm.setType(thisliteral,"boolean");
            if (arg.expression != null) 
            { expression = 
                BasicExpression.newCallBasicExpression("hasNext", arg.expression);  
            } 
            return args + ".hasNext()"; 
          } 
          else 
          { if (arg.expression != null) 
            { expression = 
                BasicExpression.newCallBasicExpression(called, arg.expression);  
            }
            return args + "." + calls;
          }  
        } 
        else if ("next".equals(called) && arg.isFile())
        { ASTTerm.setType(thisliteral,"String");
            
          if (arg.expression != null) 
          { expression = 
              BasicExpression.newCallBasicExpression("getCurrent", arg.expression);  
          } 
          return args + ".getCurrent()"; 
        } 
        else if ("next".equals(called) || "nextElement".equals(called) || "nextToken".equals(called))
        { String tt = ASTTerm.getType(args); 
          System.out.println(">>> Type of " + args + " is: " + tt);  
          if ("OclIterator".equals(tt)) 
          { if (arg.expression != null) 
            { expression = 
                BasicExpression.newCallBasicExpression("next", arg.expression);  
              statement = 
                InvocationStatement.newInvocationStatement(
                  expression, new Vector()); 
            } 

            return args + ".next()"; 
          } 
          else 
          { if (arg.expression != null) 
            { expression = 
                BasicExpression.newCallBasicExpression(called, arg.expression);  
              statement = 
                InvocationStatement.newInvocationStatement(
                  expression, new Vector()); 
            }

            return args + "." + calls; 
          } 
        } 
        else if ("countTokens".equals(called))
        { String tt = ASTTerm.getType(args); 
          System.out.println(">>> Type of " + args + " is: " + tt);  
          // if ("OclIterator".equals(tt))
          ASTTerm.setType(thisliteral,"int"); 
           
          if (arg.expression != null) 
          { BasicExpression elems = 
               BasicExpression.newBasicExpression(arg.expression, 
"elements");
            expression = new UnaryExpression("->size", elems);   
          }

          return args + ".elements->size()"; 
        } 
        else if ("get".equals(called) || "elementAt".equals(called)) 
        { ASTTerm callarg = (ASTTerm) callterms.get(2);
          String tt = ASTTerm.getType(args); 
          System.out.println(">>> Type of " + args + " is: " + tt);  
          String callp = callarg.toKM3();

          String elemType = ASTTerm.getElementType(arg); 
          ASTTerm.setType(this, elemType); 

          System.out.println(">>>--->>> element type of " + this + " is " + elemType); 
          System.out.println(); 
 
          if ("Map".equals(tt))
          { if (arg.expression != null && 
                callarg.expression != null) 
            { expression = 
                new BinaryExpression("->at", arg.expression, callarg.expression);
            }   
            return args + "->at(" + callp + ")"; 
          } 
          else 
          { if (arg.expression != null && 
                callarg.expression != null) 
            { Expression unit = new BasicExpression(1); 
              Expression incr = 
                new BinaryExpression("+", callarg.expression, unit);  
              expression = 
                new BinaryExpression("->at", arg.expression, incr);
            }   
            return args + "->at(" + callp + "+1)"; 
          }  
        }  
        else if ("size".equals(called)) 
        { ASTTerm.setType(thisliteral,"int"); 

          if (arg.expression != null) 
          { expression = new UnaryExpression("->size", arg.expression); } 

          return args + "->size()"; 
        }  
        else if ("toArray".equals(called) && arg.isCollection())
        { String elemT = ASTTerm.getElementType(arg); 
          if (elemT != null) 
          { ASTTerm.setType(thisliteral,
                            "Sequence(" + elemT + ")");
          }  
          else 
          { ASTTerm.setType(thisliteral,"Sequence"); }

          if (arg.expression != null) 
          { expression = new UnaryExpression("->asSequence", arg.expression); } 

          return args + "->asSequence()"; 
        } 
        else if ("put".equals(called) && callterms.size() >= 3)
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp1 = callarg1.toKM3(); 
          String callp2 = callarg2.toKM3(); 

          if (arg.expression != null && 
              callarg1.expression != null && 
              callarg2.expression != null) 
          { Expression lhs = BasicExpression.newIndexedBasicExpression(arg.expression, callarg1.expression);
            expression = callarg2.expression;  
            statement = 
              new AssignStatement(lhs, callarg2.expression); 
          } 

          return args + "[" + callp1 + "] := " + callp2; 
        }  
        else if ("set".equals(called) && arg.isBitSet())  // BitSet
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          if (cargs.size() > 1) 
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1); 
            String callp2 = callarg2.toKM3(); 
          
            return args + " := " + args + ".subrange(1," + callp1 + ")->union(Integer.subrange(" + callp1 + "+1," + callp2 + ")->collect( true ))->union(" + args + ".subrange(" + callp2 + "+1))"; 
          }  
          else 
          { if (arg.expression != null && 
              callarg1.expression != null) 
            { Expression unit = new BasicExpression(1); 
              Expression incr = 
                new BinaryExpression("+", callarg1.expression, unit);  
              Expression lhs = BasicExpression.newIndexedBasicExpression(arg.expression, incr); 
              Expression truebe = new BasicExpression(true); 
              statement = 
                new AssignStatement(lhs, truebe); 
            } 
            return args + "[" + callp1 + "+1] := true"; 
          }  
        }
        else if ("set".equals(called) && arg.isCollection() && callterms.size() >= 3)
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp1 = callarg1.toKM3(); 
          String callp2 = callarg2.toKM3(); 

          if (arg.expression != null && 
              callarg1.expression != null && 
              callarg2.expression != null) 
          { Expression unit = new BasicExpression(1); 
            Expression incr = 
                new BinaryExpression("+", callarg1.expression, unit);  
            Expression lhs = BasicExpression.newIndexedBasicExpression(arg.expression, incr); 
            expression = callarg2.expression; 
            statement = 
              new AssignStatement(lhs, callarg2.expression); 
          } 

          return args + "[" + callp1 + "+1] := " + callp2 + ""; 
        }  
        else if ("setElementAt".equals(called) && arg.isCollection() && cargs.size() >= 2)
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp1 = callarg1.toKM3(); 
          String callp2 = callarg2.toKM3(); 

          if (arg.expression != null && 
              callarg1.expression != null && 
              callarg2.expression != null) 
          { Expression unit = new BasicExpression(1); 
            Expression incr = 
                new BinaryExpression("+", callarg2.expression, unit);  
            Expression lhs = BasicExpression.newIndexedBasicExpression(arg.expression, incr); 
            expression = callarg1.expression; 
            statement = 
              new AssignStatement(lhs, callarg1.expression); 
          } 


          return args + "[" + callp2 + "+1] := " + callp1 + ""; 
        }  
        else if ("setCharAt".equals(called) && arg.isString() && cargs.size() >= 2)
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp1 = callarg1.toKM3(); 
          String callp2 = callarg2.toKM3(); 

          if (arg.expression != null && 
              callarg1.expression != null &&
              callarg2.expression != null) 
          { Vector pars = new Vector(); 
            Expression par1 = new BinaryExpression("+", callarg1.expression, new BasicExpression(1)); 
            pars.add(par1); 
            pars.add(callarg2.expression);  
            expression = BasicExpression.newFunctionBasicExpression("setAt", arg.expression, pars); 
            statement = new AssignStatement(arg.expression, expression); 
          }

          return args + " := " + args + ".setAt(" + callp1 + "+1, " + callp2 + ")"; 
        }  
        else if ("setLength".equals(called) && arg.isString() && cargs.size() >= 1)
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          if (arg.expression != null && 
              callarg1.expression != null) 
          { Expression argsize = 
              new UnaryExpression("->size", 
                    arg.expression); 
            Expression tst = 
              new BinaryExpression("<", argsize, 
                               callarg1.expression);
            Expression incr = 
              new BinaryExpression("+", argsize, 
                               unitExpression);  
            Expression defaultValue = 
              BasicExpression.newValueBasicExpression(
                                              "\"\""); 
            Vector pars = new Vector(); 
            pars.add(incr); 
            pars.add(callarg1.expression); 
            Expression subr = 
              BasicExpression.newFunctionBasicExpression(
                "subrange", "Integer", pars); 
            Expression col =
              new BinaryExpression("->collect", subr, 
                                   defaultValue); 
            Expression ifexpr = 
              new BinaryExpression("+", arg.expression, 
                                   col); 
            Expression elseexpr = 
              BasicExpression.newFunctionBasicExpression(
                "subrange", unitExpression, 
                callarg1.expression); 
            expression = 
              new ConditionalExpression(tst, ifexpr, elseexpr); 
            statement = 
              new AssignStatement(arg.expression, expression); 
          } 
          
          return args + " := if " + args + "->size() < " + callp1 + " then " + args + " + Integer.subrange(" + args + ".size + 1," + callp1 + ")->collect(\"\\0\") else " + args + ".subrange(1," + callp1 + ") endif"; 
        }  
        else if ("reverse".equals(called) && arg.isString())
        { if (arg.expression != null) 
          { Expression rev = new UnaryExpression("->reverse", arg.expression); 
            expression = rev; 
            statement = new AssignStatement(arg.expression, rev); 
          }
          return args + " := " + args + "->reverse()"; 
        }  
        else if ("add".equals(called) && arg.isCollection())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          if (cargs.size() > 0)
          { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3(); 
            if (cargs.size() > 1)
            { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
              String callp2 = callarg2.toKM3(); 

              if (arg.expression != null && 
                  callarg1.expression != null &&
                  callarg2.expression != null) 
              { Vector pars = new Vector(); 
                Expression par1 = new BinaryExpression("+", callarg1.expression, new BasicExpression(1)); 
                pars.add(par1); 
                pars.add(callarg2.expression);  
                expression = BasicExpression.newFunctionBasicExpression("insertAt", arg.expression, pars); 
                statement = new AssignStatement(arg.expression, expression); 
              }

              return args + " := " + args + ".insertAt(" + callp1 + " + 1, " + callp2 + ")"; 
            } 
            else 
            { if (arg.expression != null && 
                  callarg1.expression != null) 
              { Expression incl = new BinaryExpression("->including", arg.expression, callarg1.expression); 
                statement = new AssignStatement(arg.expression, incl);
                expression = incl; // not the query form 
              } 
              
              return args + " := " + args + "->including(" + callp1 + ")"; 
            } 
          } 
        }
        else if ("add".equals(called) && arg.isNumber())
        { // ASTTerm.setType(thisliteral,"real"); 
          if (cargs.size() > 0)
          { ASTTerm.setType(this, ASTTerm.getType(arg)); 

            ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3();

            if (arg.expression != null && 
                callarg1.expression != null)
            { expression = new BinaryExpression("+", 
                                   arg.expression, 
                                   callarg1.expression); 
            } 
  
            return args + " + " + callp1;  
          } 
        }
        else if ("min".equals(called) && arg.isNumber())
        { if (cargs.size() > 0)
          { ASTTerm.setType(this, ASTTerm.getType(arg)); 

            ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3(); 
  
            if (arg.expression != null && 
                callarg1.expression != null)
            { SetExpression sexpr = new SetExpression(); 
              sexpr.addElement(arg.expression); 
              sexpr.addElement(callarg1.expression);
              expression = new UnaryExpression("->min", sexpr);  
            } 
  
            return "Set{" + args + ", " + callp1 + "}->min()";  
          } 
        }
        else if ("max".equals(called) && arg.isNumber())
        { if (cargs.size() > 0)
          { ASTTerm.setType(this, ASTTerm.getType(arg)); 
            ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3(); 
  
            if (arg.expression != null && 
                callarg1.expression != null)
            { SetExpression sexpr = new SetExpression(); 
              sexpr.addElement(arg.expression); 
              sexpr.addElement(callarg1.expression);
              expression = new UnaryExpression("->max", sexpr);  
            } 

            return "Set{" + args + ", " + callp1 + "}->max()";  
          } 
        }
        else if ("negate".equals(called) && arg.isNumber())
        { ASTTerm.setType(this, ASTTerm.getType(arg)); 

          if (arg.expression != null) 
          { expression = new UnaryExpression("-", arg.expression); } 

          return "-" + args; 
        }
        else if ("not".equals(called) && arg.isNumber())
        { ASTTerm.setType(this, ASTTerm.getType(arg)); 

          if (arg.expression != null) 
          { BinaryExpression incr = 
              new BinaryExpression("+", arg.expression, 
                                   unitExpression); 
            expression = new UnaryExpression("-", incr); 
          } 

          return "-(" + args + " + 1)"; 
        }
        else if ("toBigInteger".equals(called) && arg.isNumber())
        { ASTTerm.setType(this, "long"); 

          if (arg.expression != null) 
          { BasicExpression longType = new BasicExpression(new Type("long", null)); 
            expression = new BinaryExpression("->oclAsType", arg.expression, longType); 
           } 

           return args + "->oclAsType(long)"; 
        }
        else if ("insert".equals(called) && arg.isString())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          if (cargs.size() > 0)
          { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3(); 
            if (cargs.size() > 1)
            { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
              String callp2 = callarg2.toKM3(); 

              if (arg.expression != null && 
                  callarg1.expression != null && 
                  callarg2.expression != null) 
              { Vector pars = new Vector(); 
                pars.add(new BinaryExpression("+", 
                           callarg1.expression, 
                           unitExpression)); 
                pars.add(callarg2.expression); 
                expression = 
                  BasicExpression.newFunctionBasicExpression(
                    "insertAt", arg.expression, pars); 
                statement = 
                  new AssignStatement(arg.expression, expression); 
              } 

              return args + " := " + args + ".insertAt(" + callp1 + " + 1, (\"\"+" + callp2 + "))"; 
            } 
          } 
        }
        else if ("insertElementAt".equals(called) && arg.isCollection())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp2 = callarg2.toKM3(); 

          if (arg.expression != null && 
              callarg1.expression != null && 
              callarg2.expression != null) 
          { Vector pars = new Vector(); 
            pars.add(new BinaryExpression("+", 
                           callarg2.expression, 
                           unitExpression)); 
            pars.add(callarg1.expression); 
            expression = 
               BasicExpression.newFunctionBasicExpression(
                    "insertAt", arg.expression, pars); 
            statement = 
               new AssignStatement(arg.expression, expression); 
          } 

          return args + " := " + args + ".insertAt(" + callp2 + " + 1, " + callp1 + ")"; 
        } 
        else if ("addFirst".equals(called) && arg.isCollection())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          if (cargs.size() > 0)
          { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3();

            if (arg.expression != null && 
                callarg1.expression != null) 
            { expression = 
                new UnaryExpression("->prepend", 
                                callarg1.expression); 
              statement = 
                new AssignStatement(arg.expression, 
                                    expression); 
            } 
 
            return args + " := " + args + "->prepend(" + callp1 + ")"; 
          } 
        }
        else if (("addLast".equals(called) ||
                  "addElement".equals(called))
                 && arg.isCollection())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          if (cargs.size() > 0)
          { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3(); 

            if (arg.expression != null && 
                callarg1.expression != null) 
            { expression = 
                new UnaryExpression("->append", 
                                callarg1.expression); 
              statement = 
                new AssignStatement(arg.expression, 
                                    expression); 
            } 

            return args + " := " + args + "->append(" + callp1 + ")"; 
          } 
        }
        else if ("removeFirst".equals(called) && arg.isCollection())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          if (cargs.size() == 0)
          { 
            if (arg.expression != null) 
            { expression = 
                new UnaryExpression("->tail", 
                                arg.expression); 
              statement = 
                new AssignStatement(arg.expression, 
                                    expression); 
            } 
 
            return args + " := " + args + "->tail()"; 
          } 
        }
        else if ("removeLast".equals(called) && arg.isCollection())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          if (cargs.size() == 0)
          { if (arg.expression != null) 
            { expression = 
                new UnaryExpression("->front", 
                                arg.expression); 
              statement = 
                new AssignStatement(arg.expression, 
                                    expression); 
            }

            return args + " := " + args + "->front()"; 
          } 
        }
        else if ("addAll".equals(called) && arg.isCollection())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          if (cargs.size() > 0)
          { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3(); 
            if (cargs.size() > 1)
            { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
              String callp2 = callarg2.toKM3();

              if (arg.expression != null && 
                  callarg1.expression != null && 
                  callarg2.expression != null) 
              { Vector pars1 = new Vector(); 
                pars1.add(unitExpression); 
                pars1.add(callarg1.expression); 
                Expression subr1 = 
                  BasicExpression.newFunctionBasicExpression(
                    "subrange", arg.expression, pars1); 
                Vector pars2 = new Vector(); 
                pars2.add(new BinaryExpression("+", 
                            callarg1.expression,
                            unitExpression)); 
                Expression subr2 = 
                  BasicExpression.newFunctionBasicExpression(
                    "subrange", arg.expression, pars2); 
   
                expression = 
                  new BinaryExpression("->union", subr1, 
                    new BinaryExpression("->union", 
                      callarg2.expression, subr2)); 
                statement = 
                  new AssignStatement(arg.expression, expression); 
              } 
 
              return args + " := " + args + ".subrange(1, " + callp1 + ")->union(" + callp2 + ")->union(" + args + ".subrange(" + callp1 + "+1))"; 
            } 
            else 
            { if (arg.expression != null && 
                  callarg1.expression != null) 
              { expression = 
                  new BinaryExpression("->union", 
                                  arg.expression, 
                                  callarg1.expression); 
                statement = 
                  new AssignStatement(arg.expression, 
                                      expression); 
              } 

              return args + " := " + 
                       args + "->union(" + callp1 + ")"; 
            } 
          } 
        }
        else if ("add".equals(called) && arg.isNumber()) 
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 

          ASTTerm.setType(this, ASTTerm.getType(arg)); 

          if (arg.expression != null && callarg.expression != null)
          { expression = new BinaryExpression("+", arg.expression, callarg.expression); 
            expression.setBrackets(true); 
          } 

          return "(" + args + " + " + callp + ")"; 
        }
        else if ("divide".equals(called))  // BigInteger
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 

          ASTTerm.setType(this, ASTTerm.getType(arg)); 

          if (arg.expression != null && callarg.expression != null)
          { expression = new BinaryExpression("/", arg.expression, callarg.expression); 
            expression.setBrackets(true); 
          } 

          return "(" + args + " / " + callp + ")"; 
        }
        else if ("multiply".equals(called))  // BigInteger
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 

          ASTTerm.setType(this, ASTTerm.getType(arg)); 

          if (arg.expression != null && callarg.expression != null)
          { expression = new BinaryExpression("*", arg.expression, callarg.expression); 
            expression.setBrackets(true); 
          } 

          return "(" + args + " * " + callp + ")"; 
        }
        else if ("modInverse".equals(called) && arg.isNumber()) 
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(this, "long"); 

          if (arg.expression != null && callarg1.expression != null)
          { Vector pars = new Vector(); 
            pars.add(arg.expression); 
            pars.add(callarg1.expression); 
            expression = 
              BasicExpression.newStaticCallBasicExpression(
                "modInverse", "MathLib", pars); 
          } 

          return "MathLib.modInverse(" + args + ", " + callp1 + ")"; 
        }
        else if ("modPow".equals(called) && arg.isNumber()) 
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1); 
          String callp2 = callarg2.toKM3(); 

          ASTTerm.setType(this, "long"); 

          if (arg.expression != null && callarg1.expression != null)
          { Vector pars = new Vector(); 
            pars.add(arg.expression); 
            pars.add(callarg1.expression); 
            pars.add(callarg2.expression); 
            expression = 
              BasicExpression.newStaticCallBasicExpression(
                "modPow", "MathLib", pars); 
          } 

          return "MathLib.modPow(" + args + ", " + callp1 + ", " + callp2 + ")"; 
        }
        else if ("movePointLeft".equals(called))  
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(this, ASTTerm.getType(arg)); 

          if (arg.expression != null && callarg1.expression != null)
          { Expression powexpr = new BinaryExpression("->pow", new BasicExpression(10), callarg1.expression); 
            expression = new BinaryExpression("/", arg.expression, powexpr); 
            expression.setBrackets(true); 
          } 

          return args + " * (10->pow(-" + callp1 + "))"; 
        }
        else if ("movePointRight".equals(called))  
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          ASTTerm.setType(this, ASTTerm.getType(arg)); 


          if (arg.expression != null && callarg1.expression != null)
          { Expression powexpr = new BinaryExpression("->pow", new BasicExpression(10), callarg1.expression); 
            expression = new BinaryExpression("*", arg.expression, powexpr); 
            expression.setBrackets(true); 
          } 

          return args + " * (10->pow(" + callp1 + "))"; 
        }
        else if ("and".equals(called) && arg.isInteger())
        { ASTTerm.setType(this, "long");

          ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          if (arg.expression != null && 
              callarg1.expression != null) 
          { Vector parms = new Vector(); 
            parms.add(arg.expression); 
            parms.add(callarg1.expression); 
            expression = BasicExpression.newStaticCallBasicExpression("bitwiseAnd", "MathLib", parms); 
          } 

          return "MathLib.bitwiseAnd(" + args + ", " + callp1 + ")"; 
        } 
        else if ("or".equals(called) && arg.isInteger())
        { ASTTerm.setType(this, "long");

          ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          if (arg.expression != null && 
              callarg1.expression != null) 
          { Vector parms = new Vector(); 
            parms.add(arg.expression); 
            parms.add(callarg1.expression); 
            expression = BasicExpression.newStaticCallBasicExpression("bitwiseOr", "MathLib", parms); 
          } 

          return "MathLib.bitwiseOr(" + args + ", " + callp1 + ")"; 
        } 
        else if ("xor".equals(called) && arg.isInteger())
        { ASTTerm.setType(this, "long");

          ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          if (arg.expression != null && 
              callarg1.expression != null) 
          { Vector parms = new Vector(); 
            parms.add(arg.expression); 
            parms.add(callarg1.expression); 
            expression = BasicExpression.newStaticCallBasicExpression("bitwiseXor", "MathLib", parms); 
          } 

          return "MathLib.bitwiseXor(" + args + ", " + callp1 + ")"; 
        } 
        else if ("signum".equals(called))  
        { ASTTerm.setType(this, "int"); 

          if (arg.expression != null) 
          { expression = 
              new BinaryExpression("->compareTo", arg.expression,
                    new BasicExpression(0)); 
          }

          return args + "->compareTo(0)";
        }
        else if ("bitLength".equals(called) && arg.isInteger())  
        { ASTTerm.setType(this, "int"); 

          if (arg.expression != null) 
          { Expression tobitseq = 
              BasicExpression.newStaticCallBasicExpression(
                "toBitSequence", "MathLib", arg.expression);  
            expression = 
              new UnaryExpression("->size", tobitseq); 
          }

          return "MathLib.toBitSequence(" + args + ")->size()";
        }
        else if ("bitCount".equals(called) && arg.isInteger())  
        { ASTTerm.setType(this, "int"); 

          if (arg.expression != null) 
          { Expression tobitseq = 
              BasicExpression.newStaticCallBasicExpression(
                "toBitSequence", "MathLib", arg.expression);  
            expression = 
              new BinaryExpression("->count", tobitseq, 
                    new BasicExpression(true)); 
          }

          return "MathLib.toBitSequence(" + args + ")->count(true)";
        }
        else if ("getLowestSetBit".equals(called) && arg.isInteger())  
        { ASTTerm.setType(this, "int"); 

          if (arg.expression != null) 
          { Expression tobitseq = 
              BasicExpression.newStaticCallBasicExpression(
                "toBitSequence", "MathLib", arg.expression);
            Expression revtobitseq = 
              new UnaryExpression("->reverse", tobitseq);   
            Expression indofexpression = 
              new BinaryExpression("->indexOf", revtobitseq, 
                    new BasicExpression(true)); 
            expression = new BinaryExpression("-", 
                           indofexpression, unitExpression); 
            expression.setBrackets(true); 
          }

          return "(MathLib.toBitSequence(" + args + ")->reverse()->indexOf(true) - 1)";
        }
        else if ("subtract".equals(called) && arg.isNumber())  // BigInteger
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(this, ASTTerm.getType(arg));

          if (arg.expression != null && 
              callarg.expression != null) 
          { expression = new BinaryExpression("-", 
                                 arg.expression, 
                                 callarg.expression); 
            expression.setBrackets(true); 
          } 
 
          return "(" + args + " - " + callp + ")"; 
        }
        else if ("and".equals(called) && arg.isBitSet())  // BitSet
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          return args + " := Integer.subrange(1, Set{" + args + ".size," + callp1 + ".size}->min())->collect( _i | " + args + "[_i] and " + callp1 + "[_i] )"; 
        }
        else if ("andNot".equals(called) && arg.isBitSet())  // BitSet
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          return args + " := Integer.subrange(1, Set{" + args + ".size," + callp1 + ".size}->min())->collect( _i | " + args + "[_i] and not(" + callp1 + "[_i]) )"; 
        }
        else if ("or".equals(called) && arg.isBitSet())  // BitSet
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          return args + " := Integer.subrange(1, Set{" + args + ".size," + callp1 + ".size}->min())->collect( _i | " + args + "[_i] or " + callp1 + "[_i] )"; 
        }
        else if ("xor".equals(called) && arg.isBitSet())  // BitSet
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          return args + " := Integer.subrange(1, Set{" + args + ".size," + callp1 + ".size}->min())->collect( _i | " + args + "[_i] xor " + callp1 + "[_i] )"; 
        }
        else if ("flip".equals(called) && arg.isBitSet())  // BitSet
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          if (cargs.size() > 1) 
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1); 
            String callp2 = callarg2.toKM3(); 
          
            return args + " := " + args + ".subrange(1," + callp1 + ")->union(Integer.subrange(" + callp1 + "+1," + callp2 + ")->collect( _i | not(" + args + "[_i])))->union(" + args + ".subrange(" + callp2 + "+1))"; 
          }  
          else 
          { if (arg.expression != null && 
              callarg1.expression != null) 
            { Expression unit = new BasicExpression(1); 
              Expression incr = 
                new BinaryExpression("+", callarg1.expression, unit);  
              Expression lhs = BasicExpression.newIndexedBasicExpression(arg.expression, incr); 
              Expression notExpr = 
                new UnaryExpression("not", lhs); 
              statement = 
                new AssignStatement(lhs, notExpr); 
            } 
            return args + "[" + callp1 + "+1] := not(" + args + "[" + callp1 + "+1])"; 
          }  
        }
        else if ("cardinality".equals(called) && arg.isBitSet())  // BitSet
        { ASTTerm.setType(thisliteral,"int"); 
          if (arg.expression != null) 
          { Expression truebe = new BasicExpression(true); 
            expression = 
              new BinaryExpression("->count", 
                                   arg.expression, truebe); 
          } 
       
          return args + "->count( true )";   
        }
        else if ("putAll".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3();
          ASTTerm.setType(thisliteral,ASTTerm.getType(arg)); 
 
          if (arg.expression != null && 
              callarg.expression != null) 
          { expression = 
              new BinaryExpression("->union", 
                                   arg.expression, 
                                   callarg.expression); 
          } 
       
          return args + " := " + args + "->union(" + callp + ")"; 
        }
        else if ("charAt".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(thisliteral,"String");       

          if (arg.expression != null && 
              callarg.expression != null) 
          { Expression indx = 
              new BinaryExpression("+", callarg.expression, 
                                   unitExpression); 
            expression = 
              new BinaryExpression("->at", 
                                  arg.expression, indx); 
          } 
 
          return args + "->at(" + callp + " + 1)"; 
        }
        else if ("characters".equals(called) || "toCharArray".equals(called))
        { ASTTerm.setType(thisliteral,"Sequence(String)"); 
          if (arg.expression != null) 
          { expression = 
              new UnaryExpression("->characters", 
                                   arg.expression); 
          } 
          return args + "->characters()"; 
        }
        else if ("toUpperCase".equals(called) || "toLowerCase".equals(called))
        { ASTTerm.setType(thisliteral,"String"); 
          if (arg.expression != null) 
          { expression = 
              new UnaryExpression("->" + called, 
                                   arg.expression); 
          } 
          return args + "->" + called + "()";
        }
        else if ("indexOf".equals(called))
        { ASTTerm.setType(thisliteral,"int"); 
          
          if (cargs.size() == 1)
          { ASTTerm arg1 = (ASTTerm) cargs.get(0); 
            String callp = arg1.toKM3(); 

            if (arg.expression != null && 
                arg1.expression != null) 
            { Expression indof = 
                new BinaryExpression("->indexOf", 
                                 arg.expression, 
                                 arg1.expression); 
              expression = 
                new BinaryExpression("-", indof, 
                                     unitExpression); 
              expression.setBrackets(true); 
            } 
          
            return "(" + args + "->indexOf(" + callp + ") - 1)"; 
          }
          else if (cargs.size() == 2) 
          { ASTTerm arg1 = (ASTTerm) cargs.get(0); 
            String callp1 = arg1.toKM3(); 
            ASTTerm arg2 = (ASTTerm) cargs.get(1); 
            String callp2 = arg2.toKM3(); 
          
            if (arg.expression != null && 
                arg1.expression != null && 
                arg2.expression != null) 
            { BasicExpression elseExpr = 
                new BasicExpression(-1); 
              Vector pars1 = new Vector(); 
              pars1.add(new BinaryExpression("+", 
                          arg2.expression, unitExpression)); 
              Expression subr1 = 
                BasicExpression.newFunctionBasicExpression(
                  "subrange", arg.expression, pars1);
              Expression indof = 
                new BinaryExpression("->indexOf", 
                      subr1, arg1.expression);  
              Expression tst = 
                new BinaryExpression(">", indof, 
                      new BasicExpression(0)); 
              Expression ifExpr = 
                new BinaryExpression("+", 
                      indof, arg2.expression); 
              ifExpr = 
                new BinaryExpression("-", 
                      ifExpr, unitExpression); 
              
              expression = 
                new ConditionalExpression(tst, ifExpr, 
                                          elseExpr); 
            } 

            return "if " + args + ".subrange(" + callp2 + "+1, " + args + ".size)->indexOf(" + callp1 + "+\"\") > 0 then (" + args + ".subrange(" + callp2 + "+1, " + args + ".size)->indexOf(" + callp1 + "+\"\") + " + callp2 + " - 1) else -1 endif"; 
          }
        }
        else if ("lastIndexOf".equals(called))
        { ASTTerm.setType(thisliteral,"int"); 

          if (cargs.size() == 1)
          { ASTTerm arg1 = (ASTTerm) cargs.get(0); 
            String callp = arg1.toKM3(); 
     
            if (arg.expression != null && 
                arg1.expression != null) 
            { Expression indof = 
                new BinaryExpression("->lastIndexOf", 
                                 arg.expression, 
                                 arg1.expression); 
              expression = 
                new BinaryExpression("-", indof, 
                                     unitExpression); 
              expression.setBrackets(true); 
            } 
          
            return "(" + args + "->lastIndexOf(" + callp + ") - 1)"; }
          else if (cargs.size() == 2) 
          { ASTTerm arg1 = (ASTTerm) cargs.get(0); 
            String callp1 = arg1.toKM3(); 
            ASTTerm arg2 = (ASTTerm) cargs.get(1); 
            String callp2 = arg2.toKM3(); 

            if (arg.expression != null && 
                arg1.expression != null && 
                arg2.expression != null) 
            { Vector pars1 = new Vector();
              pars1.add(unitExpression);  
              pars1.add(new BinaryExpression("+", 
                          arg2.expression, unitExpression)); 
              Expression subr = 
                BasicExpression.newFunctionBasicExpression(
                  "subrange", arg.expression, pars1);
              Expression ifExpr = 
                new BinaryExpression("->lastIndexOf", 
                      subr, arg1.expression); 
              expression = 
                new BinaryExpression("-", 
                      ifExpr, unitExpression); 
              expression.setBrackets(true); 
            } 
          
            return "(" + args + ".subrange(1, " + callp2 + "+1)->lastIndexOf(" + callp1 + "+\"\") - 1)"; 
          }
        }
        else if ("substring".equals(called))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
          ASTTerm.setType(thisliteral,"String"); 
    
          BasicExpression unit = new BasicExpression(1); 

          if (cargs.size() > 1) 
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 

            if (arg.expression != null && 
                callarg1.expression != null && 
                callarg2.expression != null) 
            { Vector pars = new Vector(); 
              Expression incr = 
                new BinaryExpression("+", callarg1.expression, 
                                     unit); 
              pars.add(incr); 
              pars.add(callarg2.expression); 
              expression = 
                BasicExpression.newCallBasicExpression(
                     "subrange", 
                     arg.expression, pars); 
            } 

            return args + ".subrange(" + callp1 + " + 1, " + callp2 + ")";
          } 
          else 
          { if (arg.expression != null && 
                callarg1.expression != null) 
            { Vector pars = new Vector(); 
              Expression incr = 
                new BinaryExpression("+", callarg1.expression, 
                                     unit); 
              pars.add(incr); 
              Expression sze = 
                new UnaryExpression("->size", callarg1.expression); 
              pars.add(sze); 

              expression = 
                BasicExpression.newCallBasicExpression(
                     "subrange", 
                     arg.expression, pars); 
            } 
            return args + ".subrange(" + callp1 + " + 1, " + args + "->size())"; 
          } 
        }  
        else if ("replaceAll".equals(called) && arg.isString())
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
          ASTTerm.setType(thisliteral,"String"); 
    
          if (cargs.size() > 1) 
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 

            if (arg.expression != null && 
                callarg1.expression != null && 
                callarg2.expression != null) 
            { Vector pars = new Vector(); 
              pars.add(callarg1.expression); 
              pars.add(callarg2.expression); 
              expression = 
                BasicExpression.newCallBasicExpression(
                     "replaceAllMatches", 
                     arg.expression, pars); 
            } 

            return args + ".replaceAllMatches(" + callp1 + ", " + callp2 + ")";
          } 
        }  
        else if ("replaceFirst".equals(called) && arg.isString())
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
          ASTTerm.setType(thisliteral,"String"); 
    
          if (cargs.size() > 1) 
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 

            if (arg.expression != null && 
                callarg1.expression != null && 
                callarg2.expression != null) 
            { Vector pars = new Vector(); 
              pars.add(callarg1.expression); 
              pars.add(callarg2.expression); 
              expression = 
                BasicExpression.newCallBasicExpression(
                     "replaceFirstMatch", 
                     arg.expression, pars); 
            } 
          
            return args + ".replaceFirstMatch(" + callp1 + ", " + callp2 + ")";
          } 
        }  
        else if ("split".equals(called))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
          ASTTerm.setType(thisliteral,"Sequence(String)"); 

          if (arg.expression != null && 
              callarg1.expression != null) 
          { expression = 
              new BinaryExpression("->split", arg.expression, callarg1.expression); 
          } 
    
          return args + "->split(" + callp1 + ")";
        }  
        else if ("trim".equals(called))
        { ASTTerm.setType(thisliteral,"String"); 

          if (arg.expression != null) 
          { expression = 
              new UnaryExpression("->trim", arg.expression); 
          } 

          return args + "->trim()"; 
        }  
        else if ("intern".equals(called))
        { ASTTerm.setType(thisliteral,"String"); 
          expression = arg.expression; 

          return "(" + args + " + \"\")"; 
        }  
        else if ("concat".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(thisliteral,"String"); 
          if (arg.expression != null && 
              callarg.expression != null) 
          { expression = 
              new BinaryExpression("+", arg.expression, callarg.expression);
            expression.setBrackets(true);  
          } 

          return "(" + args + " + " + callp + ")"; 
        }
        else if ("endsWith".equals(called) && arg.isString())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(thisliteral,"boolean");

          if (arg.expression != null && 
              callarg.expression != null) 
          { expression = 
              new BinaryExpression("->hasSuffix", arg.expression, callarg.expression); 
          } 

          return args + "->hasSuffix(" + callp + ")";   
        }
        else if ("equals".equals(called) && 
                 "Arrays".equals(arg.literalForm()))
        { ASTTerm.setType(thisliteral,"boolean"); 
          ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp2 = callarg2.toKM3(); 
          
          if (callarg1.expression != null && 
              callarg2.expression != null) 
          { expression = 
              new BinaryExpression("=", callarg1.expression, callarg2.expression); 
          } 

          return "(" + callp1 + " = " + callp2 + ")"; 
        }
        else if ("equals".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(thisliteral,"boolean"); 
          ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
          
          if (arg.isIdentifier() && callarg1.isIdentifier() && 
              (arg.isCollection() || callarg1.isCollection())
             )
          { if (arg.expression != null && 
              callarg1.expression != null) 
            { expression = 
              new BinaryExpression("=", 
                new UnaryExpression("?", arg.expression), 
                new UnaryExpression("?", callarg1.expression)); 
            } 
            return "?" + args + " = ?" + callp1; 
          }  

          if (arg.expression != null && 
              callarg1.expression != null) 
          { expression = 
              new BinaryExpression("=", arg.expression, callarg1.expression); 
          } 

          return args + " = " + callp; 
        }
        else if ("equalsIgnoreCase".equals(called) && 
                 arg.isString())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
            
          ASTTerm.setType(thisliteral,"boolean"); 

          if (arg.expression != null && 
              callarg.expression != null) 
          { expression = 
              new BinaryExpression("=", 
                new UnaryExpression("->toLowerCase", arg.expression), 
                new UnaryExpression("->toLowerCase", callarg.expression)); 
            expression.setBrackets(true); 
          } 
            
          return "(" + args + "->toLowerCase() = " + callp + "->toLowerCase())"; 
        }
        else if ("compareTo".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(thisliteral,"int");
 
          if (arg.expression != null && 
              callarg.expression != null) 
          { expression = 
              new BinaryExpression("->compareTo", arg.expression, callarg.expression); 
          } 

          return args + "->compareTo(" + callp + ")"; 
        }
        else if ("compareToIgnoreCase".equals(called) && 
                 arg.isString())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(thisliteral,"int");
 
          if (arg.expression != null && 
              callarg.expression != null) 
          { expression = 
              new BinaryExpression("->compareTo", 
                new UnaryExpression("->toLowerCase", arg.expression), 
                new UnaryExpression("->toLowerCase", callarg.expression)); 
            expression.setBrackets(true); 
          } 

          return args + "->toLowerCase()->compareTo(" + callp + "->toLowerCase())"; 
        }
        else if ("getBytes".equals(called))
        { ASTTerm.setType(thisliteral,"Sequence(int)");

          if (arg.expression != null) 
          { Expression chvar = 
              BasicExpression.newVariableBasicExpression(
                     "_ch", 
                     new Type("String",null)); 
            Expression ue = 
              new UnaryExpression("->characters", 
                                  arg.expression); 
            UnaryExpression s2b = 
              new UnaryExpression("->char2byte", chvar); 
            BinaryExpression inarg = 
              new BinaryExpression(":", chvar, ue); 
            expression = 
            new BinaryExpression("|C", inarg, s2b); 
          } 
 
          return args + "->characters()->collect( _ch | _ch->char2byte() )"; 
        }
        else if ("getChars".equals(called))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
            
          ASTTerm.setType(thisliteral,"Sequence(String)"); 

          if (cargs.size() > 3) 
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 
            ASTTerm callarg3 = (ASTTerm) cargs.get(2);
            String callp3 = callarg3.toKM3(); 
            ASTTerm callarg4 = (ASTTerm) cargs.get(3);
            String callp4 = callarg4.toKM3(); 

            return callp3 + " := (" + callp3 + ".subrange(1," + callp4 + "))^(" + args + ".subrange(" + callp1 + "+1, " + callp2 + ")->characters())";
          } 
        }
        else if ("regionMatches".equals(called))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
            
          ASTTerm.setType(thisliteral,"boolean"); 

          if (cargs.size() > 3) 
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 
            ASTTerm callarg3 = (ASTTerm) cargs.get(2);
            String callp3 = callarg3.toKM3(); 
            ASTTerm callarg4 = (ASTTerm) cargs.get(3);
            String callp4 = callarg4.toKM3(); 

            return "(" + args + ".subrange(" + callp1 + "+1," + callp1 + "+" + callp4 + ") = " + callp2 + ".subrange(" + callp3 + "+1, " + callp3 + "+" + callp4 + "))";
          } 
        }
        else if ("subList".equals(called))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
            
          ASTTerm.setType(thisliteral,"Sequence"); 

          if (cargs.size() > 1) 
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 

            if (arg.expression != null && 
                callarg1.expression != null && 
                callarg2.expression != null) 
            { Expression par1 = 
                new BinaryExpression("+", 
                    callarg1.expression, unitExpression); 
              Vector pars = new Vector(); 
              pars.add(par1); 
              pars.add(callarg2.expression); 
              expression = 
                BasicExpression.newFunctionBasicExpression(
                  "subrange", arg.expression, pars); 
            } 

            return args + ".subrange(" + callp1 + " + 1, " + callp2 + ")";
          } 

          if (arg.expression != null && 
              callarg1.expression != null) 
          { Expression par1 = 
              new BinaryExpression("+", 
                    callarg1.expression, unitExpression); 
            Expression par2 = 
              new UnaryExpression("->size", arg.expression); 
            Vector pars = new Vector(); 
            pars.add(par1); 
            pars.add(par2); 
            expression = 
              BasicExpression.newFunctionBasicExpression(
                "subrange", arg.expression, pars); 
          } 

          return args + ".subrange(" + callp1 + " + 1, " + args + "->size())"; 
        }
        else if ("subSequence".equals(called))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
            
          ASTTerm.setType(thisliteral,"String"); 

          if (cargs.size() > 1) 
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 

            if (arg.expression != null && 
                callarg1.expression != null && 
                callarg2.expression != null) 
            { Expression par1 = 
                new BinaryExpression("+", 
                    callarg1.expression, unitExpression); 
              Vector pars = new Vector(); 
              pars.add(par1); 
              pars.add(callarg2.expression); 
              expression = 
                BasicExpression.newFunctionBasicExpression(
                  "subrange", arg.expression, pars); 
            } 

            return args + ".subrange(" + callp1 + " + 1, " + callp2 + ")";
          } 

          if (arg.expression != null && 
              callarg1.expression != null) 
          { Expression par1 = 
              new BinaryExpression("+", 
                    callarg1.expression, unitExpression); 
            Expression par2 = 
              new UnaryExpression("->size", arg.expression); 
            Vector pars = new Vector(); 
            pars.add(par1); 
            pars.add(par2); 
            expression = 
              BasicExpression.newFunctionBasicExpression(
                "subrange", arg.expression, pars); 
          } 

          return args + ".subrange(" + callp1 + " + 1, " + args + "->size())"; 
        }
        else if ("headSet".equals(called) && arg.isSet())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(thisliteral,"Set"); 

          if (arg.expression != null && 
              callarg.expression != null) 
          { Expression x0Expr = 
              BasicExpression.newVariableBasicExpression("x_0"); 
            Expression tst = 
              new BinaryExpression("<", 
                    x0Expr, callarg.expression); 
            Expression par2 = 
              new BinaryExpression(":", x0Expr, arg.expression); 
            expression = 
              new BinaryExpression("|", par2, tst); 
          } 

          return args + "->select( x_0 | x_0 < " + callp + ")"; 
        }
        else if ("tailSet".equals(called) && arg.isSet())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(thisliteral,"Set"); 

          if (arg.expression != null && 
              callarg.expression != null) 
          { Expression x0Expr = 
              BasicExpression.newVariableBasicExpression("x_0"); 
            Expression tst = 
              new BinaryExpression(">=", 
                    x0Expr, callarg.expression); 
            Expression par2 = 
              new BinaryExpression(":", x0Expr, arg.expression); 
            expression = 
              new BinaryExpression("|", par2, tst); 
          } 

          return args + "->select( x_0 | x_0 >= " + callp + ")"; 
        }
        else if ("headMap".equals(called) && arg.isMap())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(thisliteral,"Map"); 

          if (arg.expression != null && 
              callarg.expression != null) 
          { Expression keyExpr = 
              BasicExpression.newVariableBasicExpression("_key"); 
            Expression tst = 
              new BinaryExpression("<", 
                    keyExpr, callarg.expression); 
            Expression par2 = 
              new BinaryExpression(":", keyExpr, 
                new UnaryExpression("->keys", 
                                    arg.expression)); 
            Expression selExpr = 
              new BinaryExpression("|", par2, tst);
            expression = 
              new BinaryExpression("->restrict", 
                    arg.expression, selExpr);  
          } 


          return args + "->restrict( " + args + "->keys()->select( _key | _key < " + callp + ") )"; 
        }
        else if ("subMap".equals(called) && arg.isMap())
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
          ASTTerm.setType(thisliteral,"Map"); 

          if (cargs.size() > 1) 
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 

            if (arg.expression != null && 
                callarg1.expression != null && 
                callarg2.expression != null) 
            { Expression keyExpr = 
              BasicExpression.newVariableBasicExpression("_key"); 
              Expression tst1 = 
                new BinaryExpression(">=", 
                    keyExpr, callarg1.expression); 
              Expression tst2 = 
                new BinaryExpression("<", 
                    keyExpr, callarg2.expression); 
              Expression par2 = 
                new BinaryExpression(":", keyExpr, 
                  new UnaryExpression("->keys", 
                                    arg.expression)); 
              Expression selExpr = 
                new BinaryExpression("|", par2, 
                  new BinaryExpression("&", tst1, tst2));
              expression = 
                new BinaryExpression("->restrict", 
                    arg.expression, selExpr);  
            } 

            return args + "->restrict( " + args + "->keys()->select( _key | _key >= " + callp1 + " & _key < " + callp2 + ") )";
          } 

          if (arg.expression != null && 
              callarg1.expression != null) 
          { Expression keyExpr = 
              BasicExpression.newVariableBasicExpression("_key"); 
            Expression tst = 
              new BinaryExpression(">=", 
                    keyExpr, callarg1.expression); 
            Expression par2 = 
              new BinaryExpression(":", keyExpr, 
                new UnaryExpression("->keys", 
                                    arg.expression)); 
            Expression selExpr = 
              new BinaryExpression("|", par2, tst);
            expression = 
              new BinaryExpression("->restrict", 
                    arg.expression, selExpr);  
          } 

          return args + "->restrict( " + args + "->keys()->select( _key | _key >= " + callp1 + ") )"; 
        }
        else if ("removeRange".equals(called))
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
                      
          ASTTerm.setType(thisliteral,
                          ASTTerm.getType(arg)); 

          if (cargs.size() > 1) 
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 

            if (arg.expression != null && 
                callarg1.expression != null && 
                callarg2.expression != null) 
            { Vector pars = new Vector(); 
              pars.add(unitExpression); 
              pars.add(callarg1.expression); 
              Expression subr1 = 
                BasicExpression.newFunctionBasicExpression(
                  "subrange", arg.expression, pars);
              Expression par = 
                new BinaryExpression("+", callarg2.expression,
                                     unitExpression); 
              Expression subr2 = 
                BasicExpression.newFunctionBasicExpression(
                  "subrange", arg.expression, par); 
              expression = 
                new BinaryExpression("->union", subr1, subr2); 
            }

            return args + ".subrange(1," + callp1 + ")->union(" + args + ".subrange(" + callp2 + "+1))";
          } 

          expression = arg.expression; 
          return args; 
        }
        else if ("trimToSize".equals(called))
        { return "// " + args + ".trimToSize()"; }
        else if ("ensureCapacity".equals(called) && arg.isString())
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
          return "// " + args + ".ensureCapacity(" + callp1 + ")"; 
        } 
        else if ("delete".equals(called) && arg.isString())
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
                      
          ASTTerm.setType(thisliteral,"String"); 

          if (cargs.size() > 1) 
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 

            if (arg.expression != null && 
                callarg1.expression != null && 
                callarg2.expression != null) 
            { Vector pars1 = new Vector(); 
              pars1.add(unitExpression); 
              pars1.add(callarg1.expression); 
              Expression subr1 = 
                BasicExpression.newFunctionBasicExpression(
                  "subrange", arg.expression, pars1); 
              Vector pars2 = new Vector(); 
              pars2.add(new BinaryExpression("+", 
                          callarg2.expression, 
                          unitExpression)); 
              Expression subr2 = 
                BasicExpression.newFunctionBasicExpression(
                  "subrange", arg.expression, pars2); 
              expression = new BinaryExpression("+", 
                                 subr1, subr2); 
              statement = 
                new AssignStatement(arg.expression, expression); 
            } 

            return args + " := " + args + ".subrange(1," + callp1 + ") + " + 
                   args + ".subrange(" + callp2 + "+1)";
          } 
          return args; 
        }
        else if ("removeAll".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 

          if (arg.expression != null && 
              callarg.expression != null) 
          { expression = 
              new BinaryExpression("-", arg.expression, callarg.expression);
            statement = 
              new AssignStatement(arg.expression, expression);  
          } 

          return args + " := " + args + " - " + callp; 
        }
        else if (("clear".equals(called) && 
                  arg.isCollection()) || 
                 "removeAllElements".equals(called))
        { if (arg.expression != null) 
          { expression = 
              new BinaryExpression("->intersection", 
                    arg.expression, 
                    new SetExpression()); 
            statement = 
              new AssignStatement(arg.expression, 
                                  expression); 
          } 

          return args + " := " + args + "->intersection(Set{})"; 
        }
        else if ("entrySet".equals(called))
        { ASTTerm.setType(thisliteral,"Set"); 

          if (arg.expression != null) 
          { expression = 
              new UnaryExpression("->asSet", arg.expression); 
          } 

          return args + "->asSet()"; 
        }
        else if ("remove".equals(called) && 
                 (arg.isCollection() || arg.isMap())
                )
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3();
          String tt = ASTTerm.getType(args); 
          String pt = ASTTerm.getType(callarg); 
          System.out.println(">>> Type of " + args + " is: " + tt);  
          System.out.println(">>> Type of " + callp + " is: " + pt);  
          if (cargs.size() == 0) // it.remove() for iterator
          { if (arg.expression != null) 
            { expression = 
                BasicExpression.newCallBasicExpression(
                                "remove",arg.expression); 
              statement = 
                InvocationStatement.newInvocationStatement(expression, new Vector()); 
            }  
            return args + ".remove()"; 
          }
 
          if (arg.isMap() && cargs.size() > 0)
          { return args + " := " + args + "->antirestrict(Set{" + callp + "})"; } 
          else if (arg.isSet() && cargs.size() > 0)
          { ASTTerm carg1 = (ASTTerm) cargs.get(0); 
            if (arg.expression != null && 
                carg1.expression != null) 
            { expression = 
                new BinaryExpression("->excluding", 
                      arg.expression,carg1.expression); 
              statement = 
                new AssignStatement(arg.expression, expression); 
            }  
           
            return args + " := " + args + "->excluding(" + callp + ")"; 
          }
          else if (arg.isSequence() && ASTTerm.isInteger(pt) && cargs.size() > 0) 
          { ASTTerm carg1 = (ASTTerm) cargs.get(0); 
            if (arg.expression != null && 
                carg1.expression != null) 
            { expression = 
                new BinaryExpression("->excludingAt", 
                      arg.expression,
                        new BinaryExpression("+", 
                          carg1.expression, 
                          unitExpression)); 
              statement = 
                new AssignStatement(arg.expression, expression); 
            }  
            return args + " := " + args + "->excludingAt(" + callp + "+1)"; 
          }   
          else if (arg.isSequence() && cargs.size() > 0)
          { ASTTerm carg1 = (ASTTerm) cargs.get(0); 
            if (arg.expression != null && 
                carg1.expression != null) 
            { expression = 
                new BinaryExpression("->excludingFirst", 
                      arg.expression,carg1.expression); 
              statement = 
                new AssignStatement(arg.expression, expression); 
            }
            return args + " := " + args + "->excludingFirst(" + callp + ")"; 
          }   
          else 
          { return args + ".remove(" + callp + ")"; } 
        }
        else if (arg.isCollection() && 
                 ("pop".equals(called) || 
                  "take".equals(called) || 
                  "removeLast".equals(called)))
        { // _1 = _2.pop(); is 
          // _1 := _2->last() ; _2 := _2->front()

          if (arg.expression != null) 
          { expression = 
              new UnaryExpression("->front", arg.expression); 
            statement = 
              new AssignStatement(arg.expression, expression); 
          } 

          return args + " := " + args + "->front()";   
        }
        else if (arg.isCollection() && 
                 "poll".equals(called))
        { // _1 = _2.poll(); is 
          // _1 := _2->min() ; 
          // _2 := _2->excludingFirst(_2->min())

          if (arg.expression != null) 
          { Expression minelem = 
              new UnaryExpression("->min", arg.expression); 
            expression = new BinaryExpression(
              "->excludingFirst", arg.expression, minelem); 
            statement = 
              new AssignStatement(arg.expression, expression); 
          } 

          return args + " := " + args + "->excludingFirst(" + args + "->min())";   
        }
        else if ("push".equals(called) && arg.isSequence())
        { 
          ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3();

          if (arg.expression != null && 
              callarg.expression != null) 
          { expression = 
              new BinaryExpression("->append", arg.expression, callarg.expression); 
            statement = 
              new AssignStatement(arg.expression, expression); 
          } 

          return args + " := " + args + "->append(" + callp + ")";   
        }
        else if ("offer".equals(called) && arg.isSequence())
        { 
          ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3();

          if (arg.expression != null && 
              callarg.expression != null) 
          { expression = 
              new BinaryExpression("->including", arg.expression, callarg.expression); 
            statement = 
              new AssignStatement(arg.expression, expression); 
          } 

          return args + " := " + args + "->including(" + callp + ")";   
        }
        else if ("contains".equals(called) && arg.isCollection())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3();
          String tt = ASTTerm.getType(args); 
          String pt = ASTTerm.getType(callarg); 
          System.out.println(">>> Type of " + args + " is: " + tt);  
          System.out.println(">>> Type of " + callp + " is: " + pt);  
          ASTTerm.setType(thisliteral,"boolean"); 

          if ("Map".equals(tt))
          { if (arg.expression != null && 
                callarg.expression != null)
            { Expression kys = 
                new UnaryExpression("->values",
                               arg.expression);
              expression = 
                new BinaryExpression("->includes", kys, 
                                   callarg.expression); 
            } 
            return args + "->values()->includes(" + callp + ")"; 
          } 
          else 
          { if (arg.expression != null && 
                callarg.expression != null) 
            { expression = 
                new BinaryExpression("->includes", 
                      arg.expression, callarg.expression); 
            } 
            return args + "->includes(" + callp + ")"; 
          }   
        }
        else if ("contains".equals(called) && arg.isString())
        { ASTTerm.setType(thisliteral,"boolean"); 
          if (cargs.size() >= 1)
          { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3();
          
            if (arg.expression != null && 
                callarg1.expression != null)
            { Expression kys = 
                new BinaryExpression("->indexOf",
                      arg.expression, callarg1.expression);
              expression = 
                new BinaryExpression(">", kys, 
                                     zeroExpression);
              expression.setBrackets(true);  
            } 
            return "(" + args + "->indexOf(" + callp1 + ") > 0)"; 
          } 
        }
        else if (arg.isCollection() && 
                 ("isEmpty".equals(called) || 
                  "empty".equals(called)))
        { ASTTerm.setType(thisliteral,"boolean"); 

          if (arg.expression != null) 
          { expression = 
              new UnaryExpression("->isEmpty", arg.expression); 
          } 

          return args + "->isEmpty()"; 
        }
        else if ("values".equals(called) && arg.isMap())
        { ASTTerm.setType(thisliteral,"Sequence"); 

          if (arg.expression != null) 
          { expression = 
              new UnaryExpression("->values", arg.expression); 
          } 

          return args + "->values()"; 
        } // actually a Bag
        else if ("keySet".equals(called) && arg.isMap())
        { ASTTerm.setType(thisliteral,"Set");

          if (arg.expression != null) 
          { expression = 
              new UnaryExpression("->keys", arg.expression); 
          } 
 
          return args + "->keys()"; 
        }
        else if ("keys".equals(called) && arg.isMap())
        { ASTTerm.setType(thisliteral,"OclIterator");

          if (arg.expression != null) 
          { Expression ks = 
              new UnaryExpression("->keys", arg.expression); 
            expression = 
              BasicExpression.newStaticCallBasicExpression(
                "newOclIterator_Set", "OclIterator", ks); 
          } 
 
          return "OclIterator.newOclIterator_Set(" + args + "->keys())"; 
        }
        else if ("elements".equals(called) || 
                 "iterator".equals(called))  // Maps: the values
        { ASTTerm.setType(thisliteral,"OclIterator"); 
          ASTTerm callarg = (ASTTerm) callterms.get(2);
          String constr = "Set"; 
          if (arg.isSequence())
          { constr = "Sequence"; } 
 
          if (cargs.size() == 0) 
          { if (arg.expression != null) 
            { expression = 
                BasicExpression.newStaticCallBasicExpression(
                  "newOclIterator_" + constr, "OclIterator", 
                  arg.expression); 
            } 

            return "OclIterator.newOclIterator_" + constr + "(" + args + ")"; 
          } 
          else 
          { String callp = callarg.toKM3(); 

            if (arg.expression != null && 
                callarg.expression != null) 
            { Expression subr = 
                BasicExpression.newFunctionBasicExpression(
                  "subrange", arg.expression,
                     new BinaryExpression("+", 
                       callarg.expression, unitExpression)); 
 
              expression = 
                BasicExpression.newStaticCallBasicExpression(
                  "newOclIterator_" + constr, "OclIterator", 
                  subr); 
            }

            return "OclIterator.newOclIterator_" + constr + "(" + args + ".subrange(" + callp + "+1, " + args + ".size))"; 
          } 
        }
        else if ("listIterator".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          
          ASTTerm.setType(thisliteral,"OclIterator"); 
          
          if (cargs.size() == 0) 
          { if (arg.expression != null) 
            { expression = 
                BasicExpression.newStaticCallBasicExpression(
                  "newOclIterator_Sequence", 
                  "OclIterator", arg.expression); 
            } 

            return "OclIterator.newOclIterator_Sequence(" + args + ")"; 
          } 
          else 
          { if (arg.expression != null && 
                callarg.expression != null) 
            { Expression subr = 
                BasicExpression.newFunctionBasicExpression(
                  "subrange", arg.expression,
                     new BinaryExpression("+", 
                       callarg.expression, unitExpression)); 
 
              expression = 
                BasicExpression.newStaticCallBasicExpression(
                  "newOclIterator_Sequence", "OclIterator", 
                  subr); 
            }

            return "OclIterator.newOclIterator_Sequence(" + args + ".subrange(" + callp + "+1, " + args + ".size))"; 
          } 
        }
        else if ("containsValue".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3();
          // String tt = ASTTerm.getType(args); 
          // String pt = ASTTerm.getType(callarg); 
          ASTTerm.setType(thisliteral,"boolean"); 

          if (arg.expression != null && 
              callarg.expression != null)
          { Expression kys = 
              new UnaryExpression("->values", arg.expression);
            expression = 
              new BinaryExpression("->includes", kys, 
                                   callarg.expression); 
          } 

          return args + "->values()->includes(" + callp + ")";    
        }
        else if ("containsKey".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3();
          // String tt = ASTTerm.getType(args); 
          // String pt = ASTTerm.getType(callarg); 
          ASTTerm.setType(thisliteral,"boolean");

          if (arg.expression != null && 
              callarg.expression != null)
          { Expression kys = 
              new UnaryExpression("->keys", arg.expression);
            expression = 
              new BinaryExpression("->includes", kys, 
                                   callarg.expression); 
          } 
                                     
          return args + "->keys()->includes(" + callp + ")";    }
        else if ("containsAll".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3();
          // String tt = ASTTerm.getType(args); 
          // String pt = ASTTerm.getType(callarg); 
          ASTTerm.setType(thisliteral,"boolean"); 
          if (arg.expression != null && 
              callarg.expression != null) 
          { expression = 
              new BinaryExpression("->includesAll", 
                    arg.expression, 
                    callarg.expression); 
          }

          return args + "->includesAll(" + callp + ")"; 
        }
        else if ("firstKey".equals(called))
        { if (arg.expression != null) 
          { Expression ks = 
              new UnaryExpression("->keys", arg.expression); 
            expression = 
              new UnaryExpression("->min", ks); 
          } 
          return args + "->keys()->min()"; 
        }
        else if ("lastKey".equals(called))
        { if (arg.expression != null) 
          { Expression ks = 
              new UnaryExpression("->keys", arg.expression); 
            expression = 
              new UnaryExpression("->max", ks); 
          } 
          return args + "->keys()->max()";
        }
        else if ("setSize".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3();
          if (arg.expression != null && 
              callarg.expression != null) 
          { Vector pars = new Vector(); 
            pars.add(unitExpression); 
            pars.add(callarg.expression); 
            expression = 
              BasicExpression.newCallBasicExpression(
                  "subrange", callarg.expression, pars); 
            statement = 
              new AssignStatement(arg.expression, expression); 
          } 
          return args + " := " + args + ".subrange(1," + callp + ");"; 
        }   
        else if ("removeElementAt".equals(called) || "deleteCharAt".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3();
          if (arg.expression != null && 
              callarg.expression != null) 
          { Expression unit = new BasicExpression(1); 
            Expression newind = 
              new BinaryExpression("+", callarg.expression, unit); 
            expression = 
              new BinaryExpression("->excludingAt", 
                                   arg.expression, 
                                   newind); 
            statement =
              new AssignStatement(arg.expression, expression); 
          } 
          return args + " := " + args + "->excludingAt(" + callp + "+1)"; 
        }   
        else if ("removeElement".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3();

          if (arg.expression != null && 
              callarg.expression != null) 
          { Expression unit = new BasicExpression(1); 
            Expression newind = 
              new BinaryExpression("+", callarg.expression, unit); 
            expression = 
              new BinaryExpression("->excludingFirst", 
                                   arg.expression, 
                                   newind); 
            statement =
              new AssignStatement(arg.expression, expression); 
          } 
              
          return args + " := " + args + "->excludingFirst(" + callp + ")"; 
        } 
        else if ("append".equals(called) && arg.isString())
        { // ASTTerm callargs = (ASTTerm) callterms.get(2);
          String callp = callargs.toKM3(); 
          if (cargs.size() == 1)
          { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3();
            if (callarg1.isStringSequence())
            { if (arg.expression != null && 
                  callarg1.expression != null) 
              { Expression sumexpr = 
                  new UnaryExpression("->sum", callarg1.expression); 
                expression = 
                  new BinaryExpression("+", arg.expression,
                                       sumexpr); 
                expression.setBrackets(true); 
                statement = 
                  new AssignStatement(arg.expression, expression); 
              }
 
              return args + 
                " := (" + args + " + " + callp1 + "->sum())"; 
            }   

            if (arg.expression != null && 
                callarg1.expression != null) 
            { expression = 
                  new BinaryExpression("+", arg.expression,
                                       callarg1.expression); 
              expression.setBrackets(true); 
              statement = 
                new AssignStatement(arg.expression, expression); 
            }

            return args + " := (" + args + " + (\"\" + " + callp1 + "))"; 
          } 
          else if (cargs.size() >= 3) 
          { // subrange of first argument
            ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3(); 
            ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 
            ASTTerm callarg3 = (ASTTerm) cargs.get(2);
            String callp3 = callarg3.toKM3();

            if (arg.expression != null && 
                callarg1.expression != null && 
                callarg2.expression != null && 
                callarg3.expression != null) 
            { Vector pars = new Vector(); 
              pars.add(new BinaryExpression("+", 
                             callarg2.expression, 
                             unitExpression)); 
              pars.add(new BinaryExpression("+", 
                             callarg2.expression, 
                             callarg3.expression)); 

              Expression subr = 
                BasicExpression.newFunctionBasicExpression(
                  "subrange", callarg1.expression, pars);
              Expression ue = 
                new UnaryExpression("->sum", subr); 
              expression = 
                new BinaryExpression("+", arg.expression, ue); 
              statement = 
                new AssignStatement(arg.expression, expression);  
            } 
 
            return args + " := (" + args + " + " + callp1 + ".subrange(" + callp2 + "+1, " + callp2 + "+" + callp3 + ")->sum())"; 
          }   

          return args + " := (" + args + " + StringLib.newString(" + callp + "))";  
        }  
        else if ("insert".equals(called) && arg.isString())
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          if (cargs.size() == 2)
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 

            if (arg.expression != null &&  
                callarg1.expression != null && 
                callarg2.expression != null) 
            { Vector pars = new Vector(); 
              Expression incr = 
                new BinaryExpression("+", callarg1.expression,
                                     unitExpression); 
              pars.add(incr); 
              pars.add(callarg2.expression); 
              expression = 
                BasicExpression.newFunctionBasicExpression(
                  "insertAt", 
                  arg.expression, pars);  
              statement = 
                new AssignStatement(arg.expression, expression); 
            } 

            return args + " := " + args + ".insertAt(" + callp1 + " +1, \"\" + " + callp2 + ")"; 
          } 
          else if (cargs.size() >= 4)
          { // subrange of 2nd argument
            ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 
            ASTTerm callarg3 = (ASTTerm) cargs.get(2);
            String callp3 = callarg3.toKM3(); 
            ASTTerm callarg4 = (ASTTerm) cargs.get(3);
            String callp4 = callarg4.toKM3(); 
            return args + " := " + args + ".insertAt(" + callp1 + "+1, " + callp2 + ".subrange(" + callp3 + "+1, " + callp3 + "+" + callp4 + ")->sum())"; 
          }     

          return args + " := " + args + ".insertAt(" + callp1 + " +1, " + callp1 + ")";  
        }
        else if ("replace".equals(called) && arg.isString() && cargs.size() == 3)
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 
          ASTTerm callarg2 = (ASTTerm) cargs.get(1);
          String callp2 = callarg2.toKM3(); 
          ASTTerm callarg3 = (ASTTerm) cargs.get(2);
          String callp3 = callarg3.toKM3(); 
          
          return args + " := (" + args + ".subrange(1," + callp1 + ") + " + args + ".subrange(" + callp2 + " + 1)).insertAt(" + callp1 + " +1, " + callp3 + ")";  
        }
        else if ("retainAll".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          if (arg.expression != null && 
              callarg.expression != null) 
          { expression = 
              new BinaryExpression("->intersection", 
                                   arg.expression, 
                                   callarg.expression); 
            statement = 
              new AssignStatement(arg.expression, expression); 
          } 

          return args + " := " + args + "->intersection(" + callp + ")"; 
        }
        else if ("removeIf".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(this, ASTTerm.getType(arg)); 
          String elemType = ASTTerm.getElementType(arg);
 
          if (arg.expression != null && 
              callarg.expression != null) 
          { callarg.expression.setBrackets(true);
            if (callarg.expression instanceof UnaryExpression)
            { ((UnaryExpression) callarg.expression).setKeyType(elemType);

              ((UnaryExpression) callarg.expression).setElementType("boolean"); 
            } 
 
            BasicExpression varexpr = 
              BasicExpression.newVariableBasicExpression( 
                                   "_var");
            BinaryExpression rlhs = 
              new BinaryExpression(":", varexpr, 
                                   arg.expression);
            BinaryExpression rrhs = 
              new BinaryExpression("->apply", 
                          callarg.expression, varexpr);   
            expression = 
              new BinaryExpression("|R", rlhs, rrhs); 
            statement = 
              new AssignStatement(arg.expression, expression); 
          } 

          return args + " := " + args + "->reject(_var | (" + callp + ")->apply(_var))"; 
        }
        else if ("filter".equals(called) && arg.isCollection())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(this, ASTTerm.getType(arg)); 
          String elemType = ASTTerm.getElementType(arg);
 
          if (arg.expression != null && 
              callarg.expression != null) 
          { callarg.expression.setBrackets(true);
            if (callarg.expression instanceof UnaryExpression)
            { ((UnaryExpression) callarg.expression).setKeyType(elemType);

              ((UnaryExpression) callarg.expression).setElementType("boolean"); 
            } 
 
            BasicExpression varexpr = 
              BasicExpression.newVariableBasicExpression( 
                                   "_var");
            BinaryExpression rlhs = 
              new BinaryExpression(":", varexpr, 
                                   arg.expression);
            BinaryExpression rrhs = 
              new BinaryExpression("->apply", 
                          callarg.expression, varexpr);   
            expression = 
              new BinaryExpression("|", rlhs, rrhs); 
            statement = 
              new AssignStatement(arg.expression, expression); 
          } 

          return args + " := " + args + "->select(_var | (" + callp + ")->apply(_var))"; 
        }
        else if ("mapToInt".equals(called) && arg.isCollection())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(this, ASTTerm.getType(arg)); 
          String elemType = ASTTerm.getElementType(arg);
 
          if (arg.expression != null && 
              callarg.expression != null) 
          { callarg.expression.setBrackets(true);
            if (callarg.expression instanceof UnaryExpression)
            { ((UnaryExpression) callarg.expression).setKeyType(elemType);

              ((UnaryExpression) callarg.expression).setElementType("int"); 
            } 
 
            BasicExpression varexpr = 
              BasicExpression.newVariableBasicExpression( 
                                   "_var");
            BinaryExpression rlhs = 
              new BinaryExpression(":", varexpr, 
                                   arg.expression);
            BinaryExpression rrhs = 
              new BinaryExpression("->apply", 
                          callarg.expression, varexpr);   
            expression = 
              new BinaryExpression("|C", rlhs, rrhs); 
            statement = 
              new AssignStatement(arg.expression, expression); 
          } 

          return args + " := " + args + "->collect(_var | (" + callp + ")->apply(_var))"; 
        }
        else if ("mapToLong".equals(called) && arg.isCollection())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(this, ASTTerm.getType(arg)); 
          String elemType = ASTTerm.getElementType(arg);
 
          if (arg.expression != null && 
              callarg.expression != null) 
          { callarg.expression.setBrackets(true);
            if (callarg.expression instanceof UnaryExpression)
            { ((UnaryExpression) callarg.expression).setKeyType(elemType);

              ((UnaryExpression) callarg.expression).setElementType("long"); 
            } 
 
            BasicExpression varexpr = 
              BasicExpression.newVariableBasicExpression( 
                                   "_var");
            BinaryExpression rlhs = 
              new BinaryExpression(":", varexpr, 
                                   arg.expression);
            BinaryExpression rrhs = 
              new BinaryExpression("->apply", 
                          callarg.expression, varexpr);   
            expression = 
              new BinaryExpression("|C", rlhs, rrhs); 
            statement = 
              new AssignStatement(arg.expression, expression); 
          } 

          return args + " := " + args + "->collect(_var | (" + callp + ")->apply(_var))"; 
        }
        else if ("mapToDouble".equals(called) && arg.isCollection())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(this, ASTTerm.getType(arg)); 
          String elemType = ASTTerm.getElementType(arg);
 
          if (arg.expression != null && 
              callarg.expression != null) 
          { callarg.expression.setBrackets(true);
            if (callarg.expression instanceof UnaryExpression)
            { ((UnaryExpression) callarg.expression).setKeyType(elemType);

              ((UnaryExpression) callarg.expression).setElementType("double"); 
            } 
 
            BasicExpression varexpr = 
              BasicExpression.newVariableBasicExpression( 
                                   "_var");
            BinaryExpression rlhs = 
              new BinaryExpression(":", varexpr, 
                                   arg.expression);
            BinaryExpression rrhs = 
              new BinaryExpression("->apply", 
                          callarg.expression, varexpr);   
            expression = 
              new BinaryExpression("|C", rlhs, rrhs); 
            statement = 
              new AssignStatement(arg.expression, expression); 
          } 

          return args + " := " + args + "->collect(_var | (" + callp + ")->apply(_var))"; 
        }
        else if ("map".equals(called) && arg.isCollection())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(this, ASTTerm.getType(arg)); 
          String elemType = ASTTerm.getElementType(arg);
 
          if (arg.expression != null && 
              callarg.expression != null) 
          { callarg.expression.setBrackets(true);
            if (callarg.expression instanceof UnaryExpression)
            { ((UnaryExpression) callarg.expression).setKeyType(elemType); } 
 
            BasicExpression varexpr = 
              BasicExpression.newVariableBasicExpression( 
                                   "_var");
            BinaryExpression rlhs = 
              new BinaryExpression(":", varexpr, 
                                   arg.expression);
            BinaryExpression rrhs = 
              new BinaryExpression("->apply", 
                          callarg.expression, varexpr);   
            expression = 
              new BinaryExpression("|C", rlhs, rrhs); 
            statement = 
              new AssignStatement(arg.expression, expression); 
          } 

          return args + " := " + args + "->collect(_var | (" + callp + ")->apply(_var))"; 
        }
        else if ("apply".equals(called) && arg.isFunction())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
        
          if (arg.expression != null && 
              callarg.expression != null) 
          { arg.expression.setBrackets(true);
            expression = 
              new BinaryExpression("->apply", 
                          arg.expression, callarg.expression);   
          } 

          return "(" + args + ")->apply(" + callp + ")"; 
        }
        else if ("sum".equals(called) && arg.isCollection())
        { if (arg.expression != null) 
          { arg.expression.setBrackets(true);
            expression = 
              new UnaryExpression("->sum", 
                                  arg.expression);   
          } 

          return "(" + args + ")->sum()"; 
        }
        else if ("stream".equals(called) && arg.isCollection())
        { ASTTerm.setType(this, ASTTerm.getType(arg)); 

          expression = arg.expression; 
          return args; 
        }
        else if ("setCharAt".equals(called) && arg.isString())
        { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
          String callp1 = callarg1.toKM3(); 

          if (cargs.size() == 2)
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 
          
            if (arg.expression != null &&  
                callarg1.expression != null && 
                callarg2.expression != null) 
            { Vector pars = new Vector(); 
              Expression incr = 
                new BinaryExpression("+", callarg1.expression,
                                     unitExpression); 
              pars.add(incr); 
              pars.add(callarg2.expression); 
              expression = 
                BasicExpression.newFunctionBasicExpression(
                  "setAt", 
                  arg.expression, pars);  
              statement = 
                new AssignStatement(arg.expression, expression); 
            } 
          

            return args + " := " + args + ".setAt(" + callp1 + " +1, (\"\" + " + callp2 + "))"; 
          } 
        }     
        else if ("println".equals(called) && "System.out".equals(arg.literalForm()))
        { if (cargs.size() == 0)
          { Vector pars = new Vector(); 
            Expression par = 
                BasicExpression.newValueBasicExpression("\"\""); 
            expression = 
                new UnaryExpression("->display", par); 
            statement = 
                new ImplicitInvocationStatement(expression); 

            return "execute (\"\")->display()"; 
          }

          ASTTerm arg1 = (ASTTerm) cargs.get(0); 
          String res = arg1.queryForm(); 

          if (arg1.expression != null) 
          { arg1.expression.setBrackets(true); 
            expression = 
                new UnaryExpression("->display", arg1.expression); 
            statement = 
                new ImplicitInvocationStatement(expression); 
          } 
            
          return "execute (" + res + ")->display()"; 
        } 
        else if ("println".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3();

          if (cargs.size() == 0)
          { if (arg.expression != null) 
            { Vector pars = new Vector(); 
              Expression par = 
                BasicExpression.newValueBasicExpression("\"\""); 
              pars.add(par); 
              expression = 
                BasicExpression.newCallBasicExpression("println", arg.expression, pars); 
              statement = 
                InvocationStatement.newInvocationStatement(expression, pars); 
            } 
            return args + ".println(\"\")"; 
          }
 
          ASTTerm callargument1 = (ASTTerm) cargs.get(0);
          String callp1 = callargument1.toKM3(); 
 
          if (callargument1.isStringSequence())
          { if (arg.expression != null && 
                callargument1.expression != null) 
            { Expression sumexpr = 
                new UnaryExpression("->sum", callargument1.expression); 
              Vector pars = new Vector(); 
              pars.add(sumexpr); 
              expression = 
                 BasicExpression.newCallBasicExpression("println", arg.expression, pars);
              statement = 
                InvocationStatement.newInvocationStatement(expression, pars); 
            } 

            return args + ".println(" + callp1 + "->sum())"; 
          }
 
          if (callargument1.isIntegerSequence() && cargs.size() == 3) 
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 
            ASTTerm callarg3 = (ASTTerm) cargs.get(2);
            String callp3 = callarg3.toKM3(); 
            return args + ".println(" + callp1 + ".subrange(" + callp2 + "+1, " + callp2 + "+" + callp3 + ")->collect( _x | _x->byte2char() )->sum())"; 
          } 

          if (arg.expression != null && 
              callargument1.expression != null) 
          { Expression emptyStr = 
                BasicExpression.newValueBasicExpression("\"\""); 
            Expression sumexpr = 
                new BinaryExpression("+", callargument1.expression, emptyStr); 
            Vector pars = new Vector(); 
            pars.add(sumexpr); 
            expression = 
                 BasicExpression.newCallBasicExpression("println", arg.expression, pars);
            statement = 
                InvocationStatement.newInvocationStatement(expression, pars); 
          } 
             
          return args + ".println(\"\"+" + callp + ")"; 
        }
        else if ("print".equals(called))
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3();
          ASTTerm callargument1 = (ASTTerm) cargs.get(0);
          String callp1 = callargument1.toKM3(); 
 
          if (callargument1.isStringSequence())
          { if (arg.expression != null && 
                callargument1.expression != null) 
            { Expression sumexpr = 
                new UnaryExpression("->sum", callargument1.expression); 
              Vector pars = new Vector(); 
              pars.add(sumexpr); 
              expression = 
                 BasicExpression.newCallBasicExpression("print", arg.expression, pars);
              statement = 
                InvocationStatement.newInvocationStatement(expression, pars); 
            } 
            return args + ".print(" + callp1 + "->sum())"; 
          }
 
          if (callargument1.isIntegerSequence() && cargs.size() == 3) 
          { ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3(); 
            ASTTerm callarg3 = (ASTTerm) cargs.get(2);
            String callp3 = callarg3.toKM3(); 
            return args + ".print(" + callp1 + ".subrange(" + callp2 + "+1, " + callp2 + "+" + callp3 + ")->collect( _x | _x->byte2char() )->sum())"; 
          } 
             
          if (arg.expression != null && 
              callargument1.expression != null) 
          { Expression emptyStr = 
                BasicExpression.newValueBasicExpression("\"\""); 
            Expression sumexpr = 
                new BinaryExpression("+", callargument1.expression, emptyStr); 
            Vector pars = new Vector(); 
            pars.add(sumexpr); 
            expression = 
                 BasicExpression.newCallBasicExpression("print", arg.expression, pars);
            statement = 
                InvocationStatement.newInvocationStatement(expression, pars); 
          } 

          return args + ".print(\"\"+" + callp + ")"; 
        }
        else if (("printf".equals(called) || 
                  "format".equals(called)) && 
                 arg.isOclFile())
        { ASTTerm callargument1 = (ASTTerm) cargs.get(0);
          String callp1 = callargument1.toKM3(); 

          SetExpression printItems = new SetExpression(true); 
          String itemseq = "Sequence{"; 
          for (int q = 1; q < cargs.size(); q++) 
          { ASTTerm cargq = (ASTTerm) cargs.get(q); 
            itemseq = itemseq + cargq.toKM3(); 
            if (q < cargs.size()-1) 
            { itemseq = itemseq + ", "; } 
            if (cargq.expression != null) 
            { printItems.addElement(cargq.expression); } 
          } 
          itemseq = itemseq + "}"; 

          if (arg.expression != null && 
              callargument1.expression != null) 
          { Vector pars = new Vector(); 
            pars.add(callargument1.expression); 
            pars.add(printItems); 
            expression = 
              BasicExpression.newCallBasicExpression("printf",
                                arg.expression, pars); 
            statement = 
              InvocationStatement.newInvocationStatement(
                                expression, pars); 
          } 
 
          return args + ".printf(" + callp1 + ", " + itemseq + ")"; 
        }  
        else if ("format".equals(called) && 
                 "String".equals(args))
        { ASTTerm callargument1 = (ASTTerm) cargs.get(0);
          String callp1 = callargument1.toKM3(); 

          SetExpression printItems = new SetExpression(true); 
          String itemseq = "Sequence{"; 
          for (int q = 1; q < cargs.size(); q++) 
          { ASTTerm cargq = (ASTTerm) cargs.get(q); 
            itemseq = itemseq + cargq.toKM3(); 
            if (q < cargs.size()-1) 
            { itemseq = itemseq + ", "; } 
            if (cargq.expression != null) 
            { printItems.addElement(cargq.expression); } 
          } 
          itemseq = itemseq + "}"; 

          if (callargument1.expression != null) 
          { Vector pars = new Vector(); 
            pars.add(callargument1.expression); 
            pars.add(printItems); 
            expression = 
              BasicExpression.newStaticCallBasicExpression(
                 "format", "StringLib", 
                 pars); 
            statement = 
              InvocationStatement.newInvocationStatement(
                                expression, pars); 
          } 
 
          return "StringLib.format(" + callp1 + ", " + itemseq + ")"; 
        }  
        else if ("getByName".equals(called) && 
                 "InetAddress".equals(args))
        { ASTTerm.setType(thisliteral,"String"); 
          ASTTerm callargument1 = (ASTTerm) cargs.get(0);
          String callp1 = callargument1.toKM3(); 

          if (callargument1.expression != null) 
          { expression = callargument1.expression; } 
 
          return callp1; 
        }  
        else if ("ready".equals(called) && arg.isFile())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 
          ASTTerm.setType(thisliteral,"boolean"); 

          if (arg.expression != null) 
          { expression =
              BasicExpression.newCallBasicExpression(
                  "canRead", arg.expression);
          }  

          return args + ".canRead()"; 
        }
        else if ("close".equals(called) && arg.isFile())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 

          if (arg.expression != null) 
          { expression =
              BasicExpression.newCallBasicExpression("closeFile", arg.expression);
            statement = 
              InvocationStatement.newInvocationStatement(expression, new Vector()); 
          }  

          return args + ".closeFile()"; 
        }
        else if ("read".equals(called) && arg.isFile())
        { if (cargs.size() == 0)
          { if (arg.expression != null) 
            { expression =
                BasicExpression.newCallBasicExpression("read", arg.expression);
            }  

            return args + ".read()"; 
          } 
          else 
          { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3();

            if (arg.expression != null && 
                callarg1.expression != null) 
            { expression =
                BasicExpression.newCallBasicExpression("readAll", arg.expression);
              statement = 
                new AssignStatement(callarg1.expression, 
                                    expression); 
            }  
 
            return callp1 + " := " + args + ".readAll()";
          }  
        }
        else if ("getInputStream".equals(called) && arg.isFile())
        { ASTTerm.setType(this,"OclFile"); 
          if (arg.expression != null) 
          { expression =  
              BasicExpression.newStaticCallBasicExpression(
                "newOclFile_Read", "OclFile", arg.expression);  
          } 
          return "OclFile.newOclFile_Read(" + args + ")"; 
        } 
        else if ("getOutputStream".equals(called) && arg.isFile())
        { ASTTerm.setType(this,"OclFile"); 
          if (arg.expression != null) 
          { expression =  
              BasicExpression.newStaticCallBasicExpression(
                "newOclFile_Write", "OclFile", arg.expression);  
          } 
          return "OclFile.newOclFile_Write(" + args + ")"; 
        } 
        else if ("next".equals(called) && arg.isFile())
        { if (cargs.size() == 0)
          { if (arg.expression != null) 
            { expression =
                BasicExpression.newCallBasicExpression("getCurrent", arg.expression);
            }  

            return args + ".getCurrent()"; 
          } 
        } 
        else if ("nextInt".equals(called) && arg.isFile())
        { if (cargs.size() == 0)
          { if (arg.expression != null) 
            { BasicExpression rexp =
                BasicExpression.newCallBasicExpression("getCurrent", arg.expression);
              expression = new UnaryExpression("->toInteger", rexp); 
            }  

            return args + ".getCurrent()->toInteger()"; 
          } 
        } 
        else if ("nextDouble".equals(called) && arg.isFile())
        { if (cargs.size() == 0)
          { if (arg.expression != null) 
            { BasicExpression rexp =
                BasicExpression.newCallBasicExpression("getCurrent", arg.expression);
              expression = new UnaryExpression("->toReal", rexp); 
            }  

            return args + ".getCurrent()->toReal()"; 
          } 
        } 
        else if ("nextBoolean".equals(called) && arg.isFile())
        { if (cargs.size() == 0)
          { if (arg.expression != null) 
            { BasicExpression rexp =
                BasicExpression.newCallBasicExpression("getCurrent", arg.expression);
              expression = new UnaryExpression("->toBoolean", rexp); 
            }  

            return args + ".getCurrent()->toBoolean()"; 
          } 
        } 
        else if ("write".equals(called) && arg.isFile())
        { if (cargs.size() == 1)
          { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3();
            if (callarg1.isStringSequence())
            { if (arg.expression != null && 
                  callarg1.expression != null) 
              { Expression par = 
                  new UnaryExpression("->sum", 
                                      callarg1.expression); 
                expression = 
                  BasicExpression.newCallBasicExpression(
                    "write", arg.expression, par);
                statement = 
                  InvocationStatement.newInvocationStatement(
                    expression, par);  
              } 

              return args + ".write(" + callp1 + "->sum())"; 
            }
        
            if (callarg1.isIntegerSequence())
            { if (arg.expression != null && 
                  callarg1.expression != null) 
              { Expression zExpr =
                  BasicExpression.newVariableBasicExpression(
                    "_z");
                Expression inarg = 
                  new BinaryExpression(":", zExpr,
                                       callarg1.expression); 
                Expression b2char = 
                  new UnaryExpression("->byte2char", zExpr);  
                Expression col = 
                  new BinaryExpression("|C", inarg, b2char); 
                Expression par = 
                  new UnaryExpression("->sum", 
                                      col); 
                expression = 
                  BasicExpression.newCallBasicExpression(
                    "write", arg.expression, par);
                statement = 
                  InvocationStatement.newInvocationStatement(
                    expression, par);  
              } 

              return args + ".write(" + callp1 + "->collect( _z | _z->byte2char() )->sum())"; 
            }        


            if (arg.expression != null && 
                callarg1.expression != null) 
            { Expression par = 
                  new BinaryExpression("+", 
                        callarg1.expression, 
                        emptyStringExpression); 
                expression = 
                  BasicExpression.newCallBasicExpression(
                    "write", arg.expression, par);
                statement = 
                  InvocationStatement.newInvocationStatement(
                    expression, par);  
            } 

            return args + ".write(\"\"+" + callp1 + ")"; 
          } 
          else if (cargs.size() == 3) 
          { ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
            String callp1 = callarg1.toKM3(); 
            ASTTerm callarg2 = (ASTTerm) cargs.get(1); 
            String callp2 = callarg2.toKM3(); 
            ASTTerm callarg3 = (ASTTerm) cargs.get(2); 
            String callp3 = callarg3.toKM3(); 
            if (callarg1.isStringSequence())
            { return args + ".write(" + callp1 + ".subrange(" + callp2 + "+1," + callp2 + "+" + callp3 + ")->sum())"; }        
            if (callarg1.isIntegerSequence())
            { return args + ".write(" + callp1 + ".subrange(" + callp2 + "+1," + callp2 + "+" + callp3 + ")->collect( _z | _z->byte2char() )->sum())"; }        
            return args + ".write(" + callp1 + ".subrange(" + callp2 + "+1," + callp2 + "+" + callp3 + "))";
          }  
        }
        else if ("newLine".equals(called) && arg.isFile())
        { if (arg.expression != null) 
          { Vector pars = new Vector(); 
            pars.add(new BasicExpression("\"\"")); 
            expression =
              BasicExpression.newCallBasicExpression("writeln", arg.expression, pars);
          }  

          return args + ".writeln(\"\")"; 
        } 
        else if ("copyInto".equals(called) && 
                 arg.isSequence())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          String callp = callarg.toKM3(); 

          if (arg.expression != null && 
              callarg.expression != null) 
          { expression = 
              new BinaryExpression("->union", 
                   new SetExpression(true), 
                   callarg.expression); 
            statement = 
              new AssignStatement(arg.expression, 
                                  expression); 
          } 

          return args + " := Sequence{}->union(" + callp + ")"; 
        }
        else if ("startsWith".equals(called) && 
                 arg.isString())
        { ASTTerm callarg = (ASTTerm) callterms.get(2); 
          
          ASTTerm.setType(thisliteral,"boolean"); 
          
          if (cargs.size() == 1) 
          { String callp = callarg.toKM3(); 

            if (arg.expression != null && 
                callarg.expression != null) 
            { expression = 
                new BinaryExpression("->hasPrefix", 
                                     arg.expression, 
                                     callarg.expression); 
            } 

            return args + "->hasPrefix(" + callp + ")"; 
          } 
          else if (cargs.size() == 2) 
          { ASTTerm callarg1 = (ASTTerm) cargs.get(0);
            String callp1 = callarg1.toKM3(); 
            ASTTerm callarg2 = (ASTTerm) cargs.get(1);
            String callp2 = callarg2.toKM3();

            if (arg.expression != null && 
                callarg1.expression != null && 
                callarg2.expression != null) 
            { Expression subr = 
                BasicExpression.newFunctionBasicExpression(
                  "subrange", arg.expression, 
                     new BinaryExpression("+", 
                           callarg2.expression, 
                           unitExpression)); 
              expression = 
                new BinaryExpression("->hasPrefix", 
                                     subr, 
                                     callarg1.expression); 
            } 

            return args + ".subrange(" + callp2 + "+1, " + args + ".size)->hasPrefix(" + callp1 + ")"; 
          } 
        }
        else if ("length".equals(called) && arg.isBitSet())
        { ASTTerm.setType(thisliteral,"int"); 

          if (arg.expression != null) 
          { expression = new BinaryExpression("->lastIndexOf", 
                                 arg.expression,
                                 new BasicExpression(true)); 
          } 

          return args + "->lastIndexOf(true)";  
        } 
        else if ("length".equals(called) || 
                 "size".equals(called) || 
                 "capacity".equals(called) ||
                 "elementCount".equals(called))
        { if (arg.isString() || arg.isCollection() || arg.isMap())
          { ASTTerm.setType(thisliteral,"int");

            if (arg.expression != null) 
            { expression = new UnaryExpression("->size", 
                                   arg.expression); 
            } 
 
            return args + "->size()";
          } 

          if (arg.expression != null) 
          { expression = 
              BasicExpression.newCallBasicExpression(
                 called, arg.expression); 
          } 

          return args + "." + called + "()";  
        }
        else if ("first".equals(called) && arg.isSet())
        { ASTTerm.setType(thisliteral,
                          ASTTerm.getElementType(arg));

          if (arg.expression != null) 
          { expression = new UnaryExpression("->min", 
                                 arg.expression); 
          } 
 
          return args + "->min()"; 
        }
        else if ("last".equals(called) && arg.isSet())
        { ASTTerm.setType(thisliteral,ASTTerm.getElementType(arg)); 

          if (arg.expression != null) 
          { expression = new UnaryExpression("->max", 
                                 arg.expression); 
          } 

          return args + "->max()"; 
        }
        else if ("getFirst".equals(called) || "first".equals(called) || "firstElement".equals(called))
        { // ASTTerm callarg = (ASTTerm) callterms.get(2); 
          // String callp = callarg.toKM3(); 
          ASTTerm.setType(thisliteral,
                          ASTTerm.getElementType(arg));
 
          if (arg.expression != null) 
          { expression = new UnaryExpression("->first", 
                                 arg.expression); 
          } 

          return args + "->first()"; 
        }
        else if ("getLast".equals(called) || "last".equals(called) || "lastElement".equals(called))
        { // ASTTerm callarg = (ASTTerm) callterms.get(2); 
          // String callp = callarg.toKM3(); 
          ASTTerm.setType(thisliteral, 
                          ASTTerm.getElementType(arg));

          if (arg.expression != null) 
          { expression = new UnaryExpression("->last", 
                                 arg.expression); 
          } 
 
          return args + "->last()"; 
        }
        else if ("peek".equals(called))
        { // ASTTerm callarg = (ASTTerm) callterms.get(2); 
          // String callp = callarg.toKM3(); 
          ASTTerm.setType(thisliteral, 
                          ASTTerm.getElementType(arg));

          if (arg.expression != null) 
          { expression = new UnaryExpression("->min", 
                                 arg.expression); 
          } 
 
          return args + "->min()"; 
        }
        else if ("toString".equals(called))
        { // ASTTerm callarg = (ASTTerm) callterms.get(2); 
          // String callp = callarg.toKM3(); 
          ASTTerm.setType(thisliteral,"String"); 

          if (arg.expression != null) 
          { Expression emptyString = 
              BasicExpression.newValueBasicExpression(
                 "\"\""); 
            expression = new BinaryExpression("+", 
                                 arg.expression,
                                 emptyString); 
            expression.setBrackets(true); 
          } 

          return "(" + args + " + \"\")"; 
        }
        else if ("currentTimeMillis".equals(called) && "System".equals(args))
        { ASTTerm.setType(thisliteral,"long"); 

          Expression dateExpr = 
            new BasicExpression(new Type("OclDate", null)); 
          
          expression = 
              BasicExpression.newCallBasicExpression(
                "getSystemTime", dateExpr); 
          
          return "OclDate.getSystemTime()"; 
        } 
        else if (arg.isDate() && 
                 ("getTimeInMillis".equals(called) || 
                  "computeTime".equals(called) || 
                  "getTime".equals(called)))
        { ASTTerm.setType(thisliteral,"long"); 

          if (arg.expression != null) 
          { expression = 
              BasicExpression.newBasicExpression(
                 arg.expression, "time"); 
          } 

          return args + ".time"; 
        } 
        else if (arg.isDate() && 
                 "setTimeInMillis".equals(called))
        { ASTTerm arg1 = (ASTTerm) cargs.get(0);
          String callp1 = arg1.toKM3(); 
       
          if (arg.expression != null && 
              arg1.expression != null) 
          { expression = 
              BasicExpression.newCallBasicExpression(
                 "setTime", arg.expression, arg1.expression); 
            statement = 
              InvocationStatement.newInvocationStatement(
                expression, arg1.expression); 
          } 

          return args + ".setTime(" + callp1 + ")"; 
        } 
        else if ("after".equals(called) && arg.isDate())
        { ASTTerm.setType(thisliteral,"boolean");  
          ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 
       
          if (arg.expression != null && 
              callarg1.expression != null) 
          { Vector pars = new Vector(); 
            pars.add(callarg1.expression); 
            expression = 
              BasicExpression.newCallBasicExpression(
                  "dateAfter", arg.expression, pars); 
          } 
            
          return args + ".dateAfter(" + callp1 + ")"; 
        }
        else if ("before".equals(called) && arg.isDate())
        { ASTTerm.setType(thisliteral,"boolean");  
          ASTTerm callarg1 = (ASTTerm) cargs.get(0);
          String callp1 = callarg1.toKM3(); 

          if (arg.expression != null && 
              callarg1.expression != null) 
          { Vector pars = new Vector(); 
            pars.add(callarg1.expression); 
            expression = 
              BasicExpression.newCallBasicExpression(
                  "dateAfter", arg.expression, pars); 
          } 
            
          return args + ".dateBefore(" + callp1 + ")"; 
        }
        else if (("Calendar".equals(args) ||
                  "GregorianCalendar".equals(args))
                 && "getInstance".equals(called))
        { ASTTerm.setType(thisliteral,"OclDate");  
          expression = 
            BasicExpression.newStaticCallBasicExpression(
              "newOclDate", "OclDate"); 
          return "OclDate.newOclDate()"; 
        } 

        System.out.println(">>> internal method call: " + arg + "." + called + cargs); 

        if (arg.expression != null) 
        { Vector pars = new Vector(); 
          for (int i = 0; i < cargs.size(); i++) 
          { ASTTerm cargi = (ASTTerm) cargs.get(i); 
            if (cargi.expression != null) 
            { pars.add(cargi.expression); } 
          } 
          expression = BasicExpression.newCallBasicExpression(
                called, arg.expression, pars); 
          ((BasicExpression) expression).setIsEvent();  
          statement = 
            InvocationStatement.newInvocationStatement(
                expression, pars);  
              
          System.out.println(">>> internal method call: " + arg.expression + "." + called + pars); 
          return args + "." + calls; 
        } 
      }
      else // call is ASTCompositeTerm 
      { Vector pars = callterm.getParameterExpressions();
        ASTTerm mterm = (ASTTerm) ((ASTCompositeTerm) call).terms.get(0); 
        String called = mterm.toKM3();

        if (arg.expression != null)   
        { expression = BasicExpression.newCallBasicExpression(called, arg.expression, pars); 

          if (ModelElement.lookupByName(args, ASTTerm.entities) != null) 
          { expression.setStatic(true);
            ((BasicExpression) expression).setIsEvent();  
            arg.expression.setUmlKind(Expression.CLASSID); 
          } 
          statement = 
            InvocationStatement.newInvocationStatement(
                                 expression, pars);  
        } 

        return args + "." + calls; 
      } 
    }

    if ("System".equals(args) && "out".equals(calls))
    { ASTTerm.setType(thisliteral,"OclFile");  
      Entity ocltypeent = new Entity("OclFile"); 
      Type ocltype = new Type(ocltypeent); 
      expression = new BasicExpression(ocltype); 
      expression.setUmlKind(Expression.CLASSID);
      BasicExpression sysfile = new BasicExpression("\"System.out\""); 
      sysfile.setType(new Type("String", null)); 
 
      ((BasicExpression) expression).setArrayIndex(sysfile); 
          
      return "OclFile[\"System.out\"]"; 
    } 
    else if ("System".equals(args) && "in".equals(calls))
    { ASTTerm.setType(thisliteral,"OclFile");  
      Entity ocltypeent = new Entity("OclFile"); 
      Type ocltype = new Type(ocltypeent); 
      expression = new BasicExpression(ocltype); 
      expression.setUmlKind(Expression.CLASSID);
      BasicExpression sysfile = new BasicExpression("\"System.in\""); 
      sysfile.setType(new Type("String", null)); 
 
      ((BasicExpression) expression).setArrayIndex(sysfile); 
      return "OclFile[\"System.in\"]"; 
    } 
    else if ("System".equals(args) && "err".equals(calls))
    { ASTTerm.setType(thisliteral,"OclFile");  
      Entity ocltypeent = new Entity("OclFile"); 
      Type ocltype = new Type(ocltypeent); 
      expression = new BasicExpression(ocltype); 
      expression.setUmlKind(Expression.CLASSID);
      BasicExpression sysfile = new BasicExpression("\"System.err\""); 
      sysfile.setType(new Type("String", null)); 
 
      ((BasicExpression) expression).setArrayIndex(sysfile); 
      return "OclFile[\"System.err\"]"; 
    } 

    if (arg.expression != null) 
    { expression = BasicExpression.newBasicExpression(arg.expression, calls); 
    } 

    return args + "." + calls; 
  } 

  public Vector literalValues()
  { Vector res = new Vector(); 

    if ("enumConstants".equals(tag))
    { for (int i = 0; i < terms.size(); i++) 
      { ASTTerm lit = (ASTTerm) terms.get(i); 
        if (",".equals(lit + "")) { } 
        else
        { res.add(lit.toKM3()); } 
      } 
    }
    return res;   
  }

  public Vector getParameterExpressions()
  { Vector res = new Vector(); 
    if ("expressionList".equals(tag))
    { 
      for (int i = 0; i < terms.size(); i++) 
      { ASTTerm telem = (ASTTerm) terms.get(i); 
        if (telem instanceof ASTSymbolTerm) { } 
        else 
        { if (telem.expression != null) 
          { res.add(telem.expression); }
        }  
      } 
    }
    return res; 
  } 

  public Vector getTypeParameterTypes()
  { Vector res = new Vector(); 
    if ("typeArguments".equals(tag))
    { 
      for (int i = 0; i < terms.size(); i++) 
      { ASTTerm telem = (ASTTerm) terms.get(i); 
        if (telem instanceof ASTSymbolTerm) { } 
        else 
        { if (telem.modelElement != null) 
          { res.add(telem.modelElement); }
        }  
      } 
    }
    return res; 
  } 

  public Vector getCreatorArgumentExpressions()
  { Vector res = new Vector();
 
    if (tag.equals("classCreatorRest"))
    { int carguments = terms.size();
      if (carguments > 0 && terms.get(0) instanceof ASTCompositeTerm) 
      { ASTCompositeTerm arguments = (ASTCompositeTerm) terms.get(0); 
        return arguments.getCreatorArgumentExpressions(); 
      } 
    }
    else if (tag.equals("arguments"))
    { int carguments = terms.size(); 
      if (carguments > 1 && terms.get(1) instanceof ASTCompositeTerm)
      { ASTCompositeTerm exprlist = (ASTCompositeTerm) terms.get(1);
        return exprlist.getParameterExpressions(); 
      } 
    }

    return res; 
  }  


  public String toKM3()
  { if ("creator".equals(tag))
    { // 2 arguments
      ASTTerm cls = (ASTTerm) terms.get(0); 
      String clsname = cls.toKM3(); 
      ASTTerm.setType(this,clsname); 
      int carguments = 0; 
      Vector cargs = new Vector(); 

      ASTTerm args = (ASTTerm) terms.get(1);
      if (args instanceof ASTCompositeTerm)
      { ASTCompositeTerm argsterms = (ASTCompositeTerm) args; 
        carguments = argsterms.terms.size();
        if (carguments > 0 && argsterms.terms.get(0) instanceof ASTCompositeTerm) 
        { ASTCompositeTerm arguments = (ASTCompositeTerm) argsterms.terms.get(0); 
          carguments = arguments.terms.size(); 
          if (carguments > 1 && arguments.terms.get(1) instanceof ASTCompositeTerm)
          { ASTCompositeTerm exprlist = (ASTCompositeTerm) arguments.terms.get(1);
            cargs = getCallArguments(exprlist);  
            carguments = exprlist.terms.size(); 
          } 
          else 
          { carguments = 0; } 
        } 
        else 
        { carguments = 0; } 
      } 


      System.out.println(">>::: Constructor " + this + " has " + cargs.size() + " arguments: " + cargs); 
        
      if (args instanceof ASTCompositeTerm && 
          "arrayCreatorRest".equals(((ASTCompositeTerm) args).tag))
      { ASTCompositeTerm argsterm = (ASTCompositeTerm) args; 
        ASTTerm sze = (ASTTerm) argsterm.terms.get(1);
        String defaultValue = getDefaultValue(clsname);  
        String res = "Integer.subrange(1," + sze.toKM3() + ")->collect(" + defaultValue + ")";
        ASTTerm.setType(this,"Sequence");

        if (sze.expression != null) 
        { BasicExpression unit = new BasicExpression(1); 
          Vector pars = new Vector(); 
          pars.add(unit); 
          pars.add(sze.expression);
          
          BasicExpression rng = BasicExpression.newFunctionBasicExpression("subrange", "Integer", pars);  
          BasicExpression def = new BasicExpression(defaultValue); 
          expression = new BinaryExpression("->collect", rng, def);  
        }
        return res;  
      } 

      String args1 = args.toKM3();
 
      if ("BigInteger".equals(cls.literalForm()))
      { ASTTerm.setType(this,"long");

        if (args.expression != null) 
        { expression = new UnaryExpression("->toLong", args.expression); } 
 
        return args1 + "->toLong()"; 
      } 

      if ("BufferedReader".equals(cls.literalForm()))
      { ASTTerm.setType(this,"OclFile"); 
        ASTTerm arg1 = (ASTTerm) cargs.get(0); 
        String sarg = arg1.toKM3(); 

        expression = arg1.expression; 

        return sarg; 
      }

      if ("BufferedWriter".equals(cls.literalForm()))
      { ASTTerm.setType(this,"OclFile"); 
        ASTTerm arg1 = (ASTTerm) cargs.get(0); 
        String sarg = arg1.toKM3();
        expression = arg1.expression; 

        return sarg;  
      }

      if ("PrintStream".equals(cls.literalForm()))
      { ASTTerm.setType(this,"OclFile"); 
        ASTTerm arg1 = (ASTTerm) cargs.get(0); 
        String sarg = arg1.toKM3();
        expression = arg1.expression; 

        return sarg; 
      }

      if ("Socket".equals(cls.literalForm()) && 
          cargs.size() > 1)
      { ASTTerm.setType(this,"OclFile");
        ASTTerm arg1 = (ASTTerm) cargs.get(0); 
        String hostarg = arg1.toKM3();
        ASTTerm arg2 = (ASTTerm) cargs.get(1); 
        String portarg = arg2.toKM3();
      
        if (arg1.expression != null && 
            arg2.expression != null) 
        { Vector pars = new Vector(); 
          pars.add(arg1.expression); 
          pars.add(arg2.expression); 

          expression =   
            BasicExpression.newStaticCallBasicExpression(
              "newOclFile_Remote", "OclFile", pars); 
        } 
        return "OclFile.newOclFile_Remote(" + hostarg + "," + 
                                          portarg + ")"; 
      }  

        

      if ("FileReader".equals(cls.literalForm()))
      { ASTTerm.setType(this,"OclFile");
        ASTTerm arg1 = (ASTTerm) cargs.get(0); 
        String sarg = arg1.toKM3();
      
        if (arg1.isString())
        { if (arg1.expression != null) 
          { Vector pars = new Vector(); 
            BasicExpression par = BasicExpression.newStaticCallBasicExpression("newOclFile", "OclFile", arg1.expression); 
            pars.add(par); 
            expression = BasicExpression.newStaticCallBasicExpression("newOclFile_Read", "OclFile", pars);  
          } 
          return "OclFile.newOclFile_Read(OclFile.newOclFile" + sarg + ")"; 
        }  

        // assume argument is a file 
        if (arg1.expression != null) 
        { expression = 
             BasicExpression.newStaticCallBasicExpression("newOclFile_Read", "OclFile", arg1.expression); 
        } 
 
        return "OclFile.newOclFile_Read" + args1; 
      }

      if ("Scanner".equals(cls.literalForm()))
      { ASTTerm.setType(this,"OclFile");
        ASTTerm arg1 = (ASTTerm) cargs.get(0); 
        if (arg1.isString())
        { if (arg1.expression != null) 
          { Vector pars = new Vector(); 
            BasicExpression par = BasicExpression.newStaticCallBasicExpression("newOclFile", "OclFile", arg1.expression); 
            pars.add(par); 
            expression = BasicExpression.newStaticCallBasicExpression("newOclFile_Read", "OclFile", pars);  
          } 
          return "OclFile.newOclFile_Read(OclFile.newOclFile(" + arg1.toKM3() + "))"; 
        }

        if (arg1.expression != null) 
        { expression = 
             BasicExpression.newStaticCallBasicExpression("newOclFile_Read", "OclFile", arg1.expression); 
        } 
  
        return "OclFile.newOclFile_Read(" + arg1.toKM3() + ")"; 
      }

      if ("InputStreamReader".equals(cls.literalForm()))
      { ASTTerm.setType(this,"OclFile");
        ASTTerm arg1 = (ASTTerm) cargs.get(0); 
        if (arg1.isString())
        { if (arg1.expression != null) 
          { Vector pars = new Vector(); 
            BasicExpression par = BasicExpression.newStaticCallBasicExpression("newOclFile", "OclFile", arg1.expression); 
            pars.add(par); 
            expression = BasicExpression.newStaticCallBasicExpression("newOclFile_Read", "OclFile", pars);  
          } 
          return "OclFile.newOclFile_Read(OclFile.newOclFile" + args1 + ")"; 
        }  

        if (arg1.expression != null) 
        { expression = 
             BasicExpression.newStaticCallBasicExpression("newOclFile_Read", "OclFile", arg1.expression); 
        } 

        return "OclFile.newOclFile_Read" + args1; 
      } // _Stream perhaps. 

      if ("ObjectInputStream".equals(cls.literalForm()))
      { ASTTerm.setType(this,"OclFile");
        ASTTerm arg1 = (ASTTerm) cargs.get(0); 
        if (arg1.isString())
        { if (arg1.expression != null) 
          { Vector pars = new Vector(); 
            BasicExpression par = BasicExpression.newStaticCallBasicExpression("newOclFile", "OclFile", arg1.expression); 
            pars.add(par); 
            expression = BasicExpression.newStaticCallBasicExpression("newOclFile_ReadB", "OclFile", pars);  
          } 
          return "OclFile.newOclFile_ReadB(OclFile.newOclFile" + args1 + ")"; 
        }
  
        if (arg1.expression != null) 
        { expression = 
             BasicExpression.newStaticCallBasicExpression("newOclFile_ReadB", "OclFile", arg1.expression); 
        } 

        return "OclFile.newOclFile_ReadB" + args1; 
      } // _Stream perhaps. 

      if ("FileWriter".equals(cls.literalForm()))
      { ASTTerm.setType(this,"OclFile");
        ASTTerm arg1 = (ASTTerm) cargs.get(0); 
        if (arg1.isString())
        { if (arg1.expression != null) 
          { Vector pars = new Vector(); 
            BasicExpression par = BasicExpression.newStaticCallBasicExpression("newOclFile", "OclFile", arg1.expression); 
            pars.add(par); 
            expression = BasicExpression.newStaticCallBasicExpression("newOclFile_Write", "OclFile", pars);  
          }
          return "OclFile.newOclFile_Write(OclFile.newOclFile(" + arg1.toKM3() + "))"; 
        }
  
        if (arg1.expression != null) 
        { expression = 
             BasicExpression.newStaticCallBasicExpression("newOclFile_Write", "OclFile", arg1.expression); 
        } 

        return "OclFile.newOclFile_Write(" + arg1.toKM3() + ")"; 
      }

      if ("FileOutputStream".equals(cls.literalForm()))
      { ASTTerm.setType(this,"OclFile");
        ASTTerm arg1 = (ASTTerm) cargs.get(0); 
        if (arg1.isString())
        { ASTTerm.setType(args,"String");
          if (arg1.expression != null) 
          { expression = BasicExpression.newStaticCallBasicExpression("newOclFile", "OclFile", arg1.expression);   
          }
          return "OclFile.newOclFile(" + arg1.toKM3() + ")"; 
        }
  
        // Else, arg1 is already an OclFile 
        ASTTerm.setType(args,"OclFile");
          
        if (arg1.expression != null) 
        { expression = arg1.expression; }  

        return arg1.toKM3(); 
      }

      if ("FileInputStream".equals(cls.literalForm()))
      { ASTTerm.setType(this,"OclFile");
        ASTTerm arg1 = (ASTTerm) cargs.get(0); 
        if (arg1.isString())
        { if (arg1.expression != null) 
          { expression = BasicExpression.newStaticCallBasicExpression("newOclFile", "OclFile", arg1.expression);   
          }
          return "OclFile.newOclFile(" + arg1.toKM3() + ")"; 
        }
  
        // Else, arg1 is already an OclFile 

        if (arg1.expression != null) 
        { expression = arg1.expression; }  

        return arg1.toKM3(); 
      }

      if ("Formatter".equals(cls.literalForm()))
      { ASTTerm.setType(this,"OclFile");
        ASTTerm arg1 = (ASTTerm) cargs.get(0); 
        if (arg1.isString())
        { if (arg1.expression != null) 
          { Vector pars = new Vector(); 
            BasicExpression par = BasicExpression.newStaticCallBasicExpression("newOclFile", "OclFile", arg1.expression); 
            pars.add(par); 
            expression = BasicExpression.newStaticCallBasicExpression("newOclFile_Write", "OclFile", pars);  
          }
          return "OclFile.newOclFile_Write(OclFile.newOclFile(" + arg1.toKM3() + "))"; 
        }  

        if (arg1.expression != null) 
        { expression = 
             BasicExpression.newStaticCallBasicExpression("newOclFile_Write", "OclFile", arg1.expression); 
        } 

        return "OclFile.newOclFile_Write(" + arg1.toKM3() + ")"; 
      }

      if ("OutputStreamWriter".equals(cls.literalForm()))
      { ASTTerm.setType(this,"OclFile");
        ASTTerm arg1 = (ASTTerm) cargs.get(0); 
        if (arg1.isString())
        { if (arg1.expression != null) 
          { Vector pars = new Vector(); 
            pars.add(arg1.expression);
            Expression innerCall = 
              BasicExpression.newStaticCallBasicExpression(
                "newOclFile", "OclFile", pars);  
            expression = 
              BasicExpression.newStaticCallBasicExpression(
                "newOclFile_Write", "OclFile", innerCall);
          } 
          return "OclFile.newOclFile_Write(OclFile.newOclFile" + args1 + ")"; 
        }

        if (arg1.expression != null) 
        { Vector pars = new Vector(); 
          pars.add(arg1.expression); 
          expression = 
            BasicExpression.newStaticCallBasicExpression(
                "newOclFile_Write", "OclFile", pars);
        } 
  
        return "OclFile.newOclFile_Write" + args1; 
      } // but mark it as a stream, not a file. 

      if ("ObjectOutputStream".equals(cls.literalForm()))
      { ASTTerm.setType(this,"OclFile");
        ASTTerm arg1 = (ASTTerm) cargs.get(0); 
        if (arg1.isString())
        { ASTTerm.setType(args,"String");
          
          if (arg1.expression != null) 
          { Vector pars = new Vector(); 
            pars.add(arg1.expression);
            Expression innerCall = 
              BasicExpression.newStaticCallBasicExpression(
                "newOclFile", "OclFile", pars);  
            expression = 
              BasicExpression.newStaticCallBasicExpression(
                "newOclFile_WriteB", "OclFile", innerCall);
          } 
          return "OclFile.newOclFile_WriteB(OclFile.newOclFile" + args1 + ")"; }  

        ASTTerm.setType(args,"OclFile");
          
        if (arg1.expression != null) 
        { Vector pars = new Vector(); 
          pars.add(arg1.expression); 
          expression = 
            BasicExpression.newStaticCallBasicExpression(
                "newOclFile_WriteB", "OclFile", pars);
        } 

        return "OclFile.newOclFile_WriteB" + args1; 
      } // but mark it as a stream, not a file. 

      if ("Thread".equals(cls.literalForm()))
      { ASTTerm.setType(this,"OclProcess");
        ASTTerm arg1 = (ASTTerm) cargs.get(0);

        if (cargs.size() > 1) // Runnable, String
        { ASTTerm arg2 = (ASTTerm) cargs.get(1); 
          
          if (arg1.expression != null && 
              arg2.expression != null) 
          { Vector pars = new Vector(); 
            pars.add(arg1.expression); 
            pars.add(arg2.expression); 
            expression = 
              BasicExpression.newStaticCallBasicExpression(
                "newOclProcess", "OclProcess", pars); 
          } 
          return "OclProcess.newOclProcess" + args1; 
        }  

        if (arg1.expression != null) 
        { Vector pars = new Vector(); 
          pars.add(arg1.expression); 
          long now = (new java.util.Date()).getTime(); 
          pars.add(new BasicExpression("\"Thread-" + now + "\"")); 
            expression = 
              BasicExpression.newStaticCallBasicExpression(
                "newOclProcess", "OclProcess", pars); 
          args1 = "(" + arg1.expression + ", \"Thread-" + now + "\")"; 
        }
 
        return "OclProcess.newOclProcess" + args1; 
      } // but mark it as a stream, not a file. 

 
      if ("Boolean".equals(cls.literalForm()))
      { ASTTerm.setType(this,"boolean"); 

        ASTTerm arg1 = (ASTTerm) cargs.get(0);
        if (arg1.expression != null)
        { expression = 
            new UnaryExpression("->toBoolean", arg1.expression); 
        } 
 
        return "(" + args1 + " + \"\")->toBoolean()"; 
      } 

      if ("Integer".equals(cls.literalForm()))
      { ASTTerm.setType(this,"int"); 

        ASTTerm arg1 = (ASTTerm) cargs.get(0);
        if (arg1.expression != null)
        { expression = 
            new UnaryExpression("->toInteger", arg1.expression); 
        } 

        return "(" + args1 + " + \"\")->toInteger()"; 
      } 
      
      if ("Long".equals(cls.literalForm()))
      { ASTTerm.setType(this,"long"); 

        ASTTerm arg1 = (ASTTerm) cargs.get(0);
        if (arg1.expression != null)
        { expression = 
            new UnaryExpression("->toLong", arg1.expression); 
        } 

        return "(" + args1 + " + \"\")->toLong()"; 
      }
 
      if ("Byte".equals(cls.literalForm()))
      { ASTTerm.setType(this,"int"); 

        ASTTerm arg1 = (ASTTerm) cargs.get(0);
        if (arg1.expression != null)
        { expression = 
            new UnaryExpression("->toInteger", arg1.expression); 
        } 

        return "(" + args1 + " + \"\")->toInteger()"; 
      } 
      
      if ("Short".equals(cls.literalForm()))
      { ASTTerm.setType(this,"int"); 

        ASTTerm arg1 = (ASTTerm) cargs.get(0);
        if (arg1.expression != null)
        { expression = 
            new UnaryExpression("->toInteger", arg1.expression); 
        } 

        return "(" + args1 + " + \"\")->toInteger()"; 
      } 
      
      if ("Double".equals(cls.literalForm()))
      { ASTTerm.setType(this,"double"); 

        ASTTerm arg1 = (ASTTerm) cargs.get(0);
        if (arg1.expression != null)
        { expression = 
            new UnaryExpression("->toReal", arg1.expression); 
        } 

        return "(" + args1 + " + \"\")->toReal()"; 
      } 
      
      if ("Float".equals(cls.literalForm()))
      { ASTTerm.setType(this,"double"); 

        ASTTerm arg1 = (ASTTerm) cargs.get(0);
        if (arg1.expression != null)
        { expression = 
            new UnaryExpression("->toReal", arg1.expression); 
        } 

        return "(" + args1 + " + \"\")->toReal()"; 
      } 
      
      if ("BigDecimal".equals(cls.literalForm()))
      { ASTTerm.setType(this,"double"); 

        ASTTerm arg1 = (ASTTerm) cargs.get(0);
        if (arg1.expression != null)
        { expression = 
            new UnaryExpression("->toReal", arg1.expression); 
        } 

        return "(" + args1 + " + \"\")->toReal()"; 
      } 

      if ("Number".equals(cls.literalForm()))
      { ASTTerm.setType(this,"double"); 
        expression = new BasicExpression(0.0); 
        return "0.0"; 
      } 

      if ("Character".equals(cls.literalForm()))
      { ASTTerm.setType(this,"String"); 

        ASTTerm arg1 = (ASTTerm) cargs.get(0);
        if (arg1.expression != null)
        { Expression emptyStr = 
            BasicExpression.newValueBasicExpression("\"\""); 
          expression = 
            new BinaryExpression("+", emptyStr, arg1.expression); 
          expression.setBrackets(true); 
        } 

        return "(" + args1 + " + \"\")"; 
      } 

      if (("String".equals(cls.literalForm()) ||
           "StringBuilder".equals(cls.literalForm()) ||
           "StringBuffer".equals(cls.literalForm())) && 
          "()".equals(args.literalForm()))
      { ASTTerm.setType(this,"String");

        expression = new BasicExpression("\"\""); 
        expression.setType(new Type("String", null)); 

        return "\"\""; 
      }
      else if (("String".equals(cls.literalForm()) ||
                "StringBuilder".equals(cls.literalForm()) || 
                "StringBuffer".equals(cls.literalForm())) &&
               cargs.size() > 0 && cargs.size() < 3) 
      { ASTTerm strarg = (ASTTerm) cargs.get(0);
        String val = strarg.toKM3(); 

        ASTTerm.setType(this,"String");

        if (strarg.isString())
        { if (strarg.expression != null)
          { Expression emptyStr = 
              BasicExpression.newValueBasicExpression("\"\""); 
            expression = 
              new BinaryExpression("+", emptyStr, strarg.expression); 
            expression.setBrackets(true); 
          } 
          return "(\"\" + " + val + ")"; 
        } 
        else if (strarg.isIntegerSequence())
        { if (strarg.expression != null) 
          { Expression xvar = 
              BasicExpression.newVariableBasicExpression(
                                                     "_x");
            Expression colarg = 
              new UnaryExpression("->byte2char", xvar); 
            Expression collhs = 
              new BinaryExpression(":", xvar, 
                                   strarg.expression);  
            Expression col = 
              new BinaryExpression("|C", collhs, colarg); 
            expression = 
              new UnaryExpression("->sum", col); 
          } 
  
          return val + "->collect( _x | _x->byte2char() )->sum()"; 
        } 
        else if (strarg.isStringSequence())
        { if (strarg.expression != null) 
          { expression = new UnaryExpression("->sum", 
                                    strarg.expression); 
          }
 
          return val + "->sum()"; 
        }
        else 
        { expression = 
            BasicExpression.newValueBasicExpression("\"\""); 
          return "\"\""; 
        } // capacity
      }  
      else if ("String".equals(cls.literalForm()) && cargs.size() >= 3)
      { ASTTerm strarg = (ASTTerm) cargs.get(0);
        ASTTerm strind = (ASTTerm) cargs.get(1);
        ASTTerm strleng = (ASTTerm) cargs.get(2);
        String s1 = strarg.toKM3();
        String s2 = strind.toKM3();
        String s3 = strleng.toKM3();
        ASTTerm.setType(this,"String");

        if (strarg.isString())
        { if (strarg.expression != null && 
              strind.expression != null && 
              strleng.expression != null) 
          { Vector pars = new Vector(); 
            pars.add(new BinaryExpression("+", strind.expression, unitExpression)); 
            pars.add(new BinaryExpression("+", 
                      strind.expression, strleng.expression)); 
            expression = 
              BasicExpression.newFunctionBasicExpression(
                "subrange", strarg.expression, pars); 
          }
 
          return s1 + ".subrange(" + s2 + "+1, " + s2 + "+" + s3 + ")"; 
        } 
        else if (strarg.isIntegerSequence())
        { return s1 + ".subrange(" + s2 + "+1, " + s2 + "+" + s3 + ")->collect( _x | _x->byte2char() )->sum()"; } 
        else if (strarg.isStringSequence())
        { return s1 + ".subrange(" + s2 + "+1, " + s2 + "+" + s3 + ")->sum()"; }
      }  
      else if ("String".equals(cls.literalForm()))
      { return "StringLib.newString" + args1; }

      if ("StringTokenizer".equals(cls.literalForm()) && cargs.size() == 1)
      { ASTTerm.setType(this,"OclIterator");
        ASTTerm arg1 = (ASTTerm) cargs.get(0); 

        if (arg1.expression != null) 
        { expression = 
            BasicExpression.newStaticCallBasicExpression(
              "newOclIterator_String", "OclIterator", 
              arg1.expression); 
        } 

        return "OclIterator.newOclIterator_String" + args1; 
      }
      else if ("StringTokenizer".equals(cls.literalForm()) && cargs.size() == 2) 
      { ASTTerm.setType(this,"OclIterator");

        ASTTerm arg1 = (ASTTerm) cargs.get(0); 
        ASTTerm arg2 = (ASTTerm) cargs.get(1); 

        if (arg1.expression != null && 
            arg2.expression != null) 
        { Vector pars = new Vector(); 
          pars.add(arg1.expression); 
          pars.add(arg2.expression); 
          expression = 
            BasicExpression.newStaticCallBasicExpression(
              "newOclIterator_String_String", 
              "OclIterator", pars); 
        } 

        return "OclIterator.newOclIterator_String_String" + args1; 
      }

      if ("Set".equals(clsname) && cargs.size() == 0)
      { ASTTerm.setType(this,"Set");
        expression = new SetExpression(); 
        return "Set{}"; 
      }

      if ("Set".equals(clsname) && cargs.size() > 0)
      { ASTTerm.setType(this,"Set");
        ASTTerm strarg = (ASTTerm) cargs.get(0);
        if (strarg.isInteger())
        { expression = new SetExpression(); 
          return "Set{}"; 
        } 
        return "Set{}->union" + args1; 
      } 

      if ("Sequence".equals(clsname) && cargs.size() == 0)
      { ASTTerm.setType(this,"Sequence");
        expression = new SetExpression(true); 
        return "Sequence{}"; 
      } 
      if ("Sequence".equals(clsname) && cargs.size() > 0)
      { ASTTerm.setType(this,"Sequence");
        ASTTerm strarg = (ASTTerm) cargs.get(0);
        if (strarg.isInteger())
        { expression = new SetExpression(true); 
          return "Sequence{}"; 
        } 
        return "Sequence{}->union" + args1; 
      } 

      if ("Vector".equals(cls.literalForm()) && cargs.size() == 0)
      { ASTTerm.setType(this,"Sequence");
        expression = new SetExpression(true); 
        return "Sequence{}"; 
      } 
      if ("Vector".equals(cls.literalForm()) && cargs.size() > 0)
      { ASTTerm.setType(this,"Sequence");
        ASTTerm strarg = (ASTTerm) cargs.get(0);
        if (strarg.isInteger())
        { expression = new SetExpression(true); 
          return "Sequence{}"; 
        } 
        return "Sequence{}->union" + args1; 
      } 

      if ("BitSet".equals(cls.literalForm()) && cargs.size() == 0)
      { ASTTerm.setType(this,"Sequence(boolean)");

        Vector pars = new Vector(); 
        pars.add(unitExpression); 
        pars.add(new BasicExpression(64)); 

        Expression subr = 
          BasicExpression.newStaticCallBasicExpression(
            "subrange", "Integer", pars); 
        expression = 
          new BinaryExpression("->collect", subr,
                new BasicExpression(false)); 

        return "Integer.subrange(1,64)->collect(false)"; 
      } 
      if ("BitSet".equals(cls.literalForm()) && cargs.size() > 0)
      { ASTTerm.setType(this,"Sequence(boolean)");
        ASTTerm strarg = (ASTTerm) cargs.get(0);
        String argocl = strarg.toKM3(); 

        if (strarg.expression != null) 
        { Vector pars = new Vector(); 
          pars.add(unitExpression); 
          pars.add(strarg.expression); 

          Expression subr = 
            BasicExpression.newStaticCallBasicExpression(
              "subrange", "Integer", pars); 
          expression = 
            new BinaryExpression("->collect", subr,
                new BasicExpression(false)); 
        } 

        return "Integer.subrange(1," + argocl + ")->collect(false)"; 
      } 



      if ("Map".equals(clsname) && "()".equals(args1))
      { ASTTerm.setType(this,"Map");
        expression = new SetExpression(); 
        expression.setType(new Type("Map", null)); 

        return "Map{}"; 
      } 
      if ("Map".equals(clsname) && cargs.size() > 0)
      { ASTTerm.setType(this,"Map");
        ASTTerm strarg = (ASTTerm) cargs.get(0);

        if (strarg.isInteger())
        { expression = new SetExpression(); 
          expression.setType(new Type("Map", null)); 
          return "Map{}"; 
        }

        if (strarg.expression != null) 
        { SetExpression emptyMap = new SetExpression(); 
          emptyMap.setType(new Type("Map", null));
          expression = new BinaryExpression("->union", emptyMap, strarg.expression); 
        } 
 
        return "Map{}->union" + args1; 
      } 

      if ("Object".equals(cls.literalForm()) && "()".equals(args.literalForm()))
      { ASTTerm.setType(this,"OclAny");
        expression = new BasicExpression("\"\""); 
        expression.setType(new Type("OclAny", null)); 

        return "\"\""; 
      }

      if (clsname.endsWith("Exception") && cargs.size() > 0)
      { ASTTerm strarg = (ASTTerm) cargs.get(0);
        ASTTerm.setType(this,"OclException");
        if (strarg.isString()) { }
        else 
        { 
          if (strarg.expression != null) 
          { Expression emptyString = 
              BasicExpression.newValueBasicExpression("\"\""); 
            Expression toStr = 
              new BinaryExpression("+", emptyString, 
                                   strarg.expression); 
            expression = BasicExpression.newStaticCallBasicExpression(
              "new" + clsname, clsname, toStr);
          }  

          return clsname + ".new" + clsname + "(\"\" + " + strarg.toKM3() + ")"; 
        } 

        if (strarg.expression != null) 
        { expression = BasicExpression.newStaticCallBasicExpression("new" + clsname, clsname, strarg.expression);
        }  

        return clsname + ".new" + clsname + args1; 
      } 

      Vector creatorArguments = new Vector(); 
      if (args instanceof ASTCompositeTerm)
      { creatorArguments = 
         ((ASTCompositeTerm) args).getCreatorArgumentExpressions(); 
      } 

      if (args.expressions != null) 
      { expression = BasicExpression.newStaticCallBasicExpression("new" + clsname, clsname, args.expressions); } 
      else
      { expression = BasicExpression.newStaticCallBasicExpression("new" + clsname, clsname, creatorArguments); } 

        
      return clsname + ".new" + clsname + args1; 
    } 

    if ("createdName".equals(tag))
    { ASTTerm mainclass = (ASTTerm) terms.get(0);
      String res = mainclass.toKM3();
      ASTTerm.setType(this,res);

      Type typ; 
      Entity ent = (Entity) ModelElement.lookupByName(res, ASTTerm.entities); 
      if (ent != null) 
      { typ = new Type(ent); } 
      else if (ModelElement.lookupByName(res,ASTTerm.enumtypes) != null) 
      { typ = (Type) ModelElement.lookupByName(res,ASTTerm.enumtypes); } 
      else 
      { typ = new Type(res,null); } 

      expression = new BasicExpression(typ);
  
      return res; 
    } // Ignore type parameters in creation. 

    if ("classCreatorRest".equals(tag))
    { // (arguments ( )) or 
      // (arguments ( (expressionList ...) ))
      if (terms.size() > 0) 
      { ASTTerm t = (ASTTerm) terms.get(0); 
        String ss = t.toKM3(); 
        expression = t.expression;
        expressions = t.expressions;  
        return ss; 
      } 
    } 

    if ("arguments".equals(tag))
    { // (arguments ( )) or 
      // (arguments ( (expressionList ...) ))

      if (terms.size() > 2) 
      { ASTTerm t = (ASTTerm) terms.get(1); 
        String ss = t.toKM3(); 
        expression = t.expression; 
        if (t instanceof ASTCompositeTerm) 
        { expressions = 
            ((ASTCompositeTerm) t).getParameterExpressions();
        }  
        
        return ss; 
      } 

      expressions = new Vector(); 
      return "()"; 
    } 


    if ("literal".equals(tag))
    { System.out.println(">> Literal with " + terms.size() + " terms " + terms);
      String res = ""; 
      for (int y = 0; y < terms.size(); y++)
      { ASTTerm yt = (ASTTerm) terms.get(y); 
        System.out.println(">>> Term " + y + ": " + yt);
        if (y < terms.size() - 1) 
        { res = res + yt.toKM3() + " "; } 
        else 
        { res = res + yt.toKM3(); }  
      }  
      System.out.println();

      if (terms.size() == 1) 
      { ASTTerm tt = (ASTTerm) terms.get(0);
        expression = tt.expression; 
        ASTTerm.setType(this, ASTTerm.getType(tt)); 
      } 
 
      return res; 
    } 

    if ("variableInitializer".equals(tag))
    { ASTTerm t = (ASTTerm) terms.get(0); // An expression

      System.out.println(">> Variable initializer with " + terms.size() + " terms"); 

      for (int y = 0; y < terms.size(); y++)
      { ASTTerm yt = (ASTTerm) terms.get(y); 
        System.out.println(">>> Term " + y + ": " + yt); 
      }  
      System.out.println(); 

      String initexpr = t.toKM3(); // updateForm
      ASTTerm.setType(this,ASTTerm.getType(t)); 
      expression = t.expression;  // queryForm
        
      if (t.updatesObject(null))
      { System.out.println(">> Expression returning value, and with side-effect: " + t); 
        statement = t.statement;    // updateForm
        System.out.println(statement); 
        System.out.println(); 
        String qf = t.queryForm(); 
        expression = t.expression; 
        System.out.println(">> Query form: >> " + expression); 
        
        return qf + " ; " + initexpr; 
      }

      return initexpr; 
    }    

    if ("methodCall".equals(tag))
    { System.out.println(">> Method call with " + terms.size() + " terms " + terms);
      // methodname ( args ) 

      if (terms.size() == 4) 
      { // methodName ( pars ) 
        ASTTerm methodt = (ASTTerm) terms.get(0);
        String methodname = methodt.toKM3();
        // ASTTerm callargs = (ASTTerm) terms.get(2);
        // Vector cargs = getCallArguments(callargs); 
  
        if ("super".equals(methodname))
        { // call of superclass constructor 
          methodname = "super.initialise"; 
          BasicExpression supbe = new BasicExpression("super"); 
          supbe.setUmlKind(Expression.VARIABLE);
          BasicExpression initbe = new BasicExpression("initialise"); 
          initbe.setUmlKind(Expression.UPDATEOP);
          initbe.setIsEvent();  
          initbe.setObjectRef(supbe);
          initbe.setParameters(new Vector());  
          expression = initbe;  
          statement = new InvocationStatement(initbe); 
        }
        else 
        { expression = new BasicExpression(methodname); 
          ((BasicExpression) expression).setParameters(new Vector());  
          ((BasicExpression) expression).setIsEvent();  
          expression.setUmlKind(Expression.UPDATEOP);
          statement = new InvocationStatement((BasicExpression) expression); 
        } 

        ASTTerm pars = (ASTTerm) terms.get(2); 
        String parstring = pars.toKM3(); 
        Vector parexprs = new Vector(); 
        if (pars instanceof ASTCompositeTerm) 
        { parexprs = 
            ((ASTCompositeTerm) pars).getParameterExpressions();
        }  

        ((BasicExpression) expression).setParameters(parexprs);
        ((InvocationStatement) statement).setParameters(parexprs); 
        return methodname + parstring;  
      } 

      if (terms.size() == 3) 
      { // methodName ( ) 
        ASTTerm methodt = (ASTTerm) terms.get(0);
        String methodname = methodt.toKM3();  
        if ("super".equals(methodname))
        { // call of superclass constructor 
          methodname = "super.initialise"; 
          BasicExpression supbe = new BasicExpression("super"); 
          supbe.setUmlKind(Expression.VARIABLE);
          BasicExpression initbe = new BasicExpression("initialise"); 
          initbe.setUmlKind(Expression.UPDATEOP); 
          initbe.setObjectRef(supbe); 
          initbe.setIsEvent();  
          initbe.setParameters(new Vector());  
          expression = initbe;  
          statement = new InvocationStatement(initbe); 
        }
        else 
        { expression = new BasicExpression(methodname); 
          expression.setUmlKind(Expression.UPDATEOP);
          ((BasicExpression) expression).setParameters(new Vector());  
          ((BasicExpression) expression).setIsEvent();  
          statement = new InvocationStatement((BasicExpression) expression); 
        } 
        Vector parexprs = new Vector(); 
        ((BasicExpression) expression).setParameters(parexprs);
        ((InvocationStatement) statement).setParameters(parexprs); 
        return methodname + "()";  
      } 
        
      if (terms.size() > 2) 
      { ASTTerm methodt = (ASTTerm) terms.get(0);
        String methodname = methodt.toKM3();  
        if ("super".equals(methodname))
        { // call of superclass constructor 
          methodname = "super.initialise"; 
        }
 
        for (int i = 1; i < terms.size(); i++) 
        { ASTTerm tt = (ASTTerm) terms.get(i); 
          methodname = methodname + tt.toKM3(); 
        }
 
        return methodname; 
      }  
    } 
 
     
    if ("parExpression".equals(tag) || "primary".equals(tag))
    { System.out.println(">> parExpression with " + terms.size() + " terms " + terms);
      String res = ""; 

      if (terms.size() == 3)  // ( t ) 
      { ASTTerm tt = (ASTTerm) terms.get(1); 
        res = tt.toKM3();
        String typ = ASTTerm.getType(tt); 
        ASTTerm.setType(this,typ);

        expression = tt.expression;
        if (expression != null) 
        { expression.setBrackets(true); }   
        return "(" + res + ")";   
      } 

      if (terms.size() == 1) 
      { ASTTerm tt = (ASTTerm) terms.get(0); 
        res = tt.toKM3();
        String typ = ASTTerm.getType(tt); 
        ASTTerm.setType(this,typ);
        expression = tt.expression; 
        return res;   
      } 

      for (int i = 0; i < terms.size(); i++) 
      { res = res + ((ASTTerm) terms.get(i)).toKM3(); }
 
      return res; 
    } 

    if ("expressionList".equals(tag))
    { Vector strs = new Vector(); 
      Vector exprs = new Vector(); 

      for (int i = 0; i < terms.size(); i++) 
      { ASTTerm telem = (ASTTerm) terms.get(i); 
        if (telem instanceof ASTSymbolTerm) { } 
        else 
        { strs.add(telem.toKM3()); 
          if (telem.expression != null) 
          { exprs.add(telem.expression); }
          statement = telem.statement; // Hack
        }  
      } 
     
      String res = "( "; 
      for (int i = 0; i < strs.size(); i++) 
      { res = res + strs.get(i); 
        if (i < strs.size()-1) 
        { res = res + ","; } 
      }
 
      if (exprs.size() == 1) 
      { expression = (Expression) exprs.get(0); 
        expression.setBrackets(true); 
      } // actually a tuple of the expressions

      return res + " )"; 
    } 

    if ("lambdaParameters".equals(tag))
    { if (terms.size() > 1)
      { ASTTerm par = (ASTTerm) terms.get(1); 
        String res = par.queryForm(); 
        expression = par.expression; 
        return res; 
      } 
    } 

    if ("lambdaExpression".equals(tag) && terms.size() > 2)
    { // (lambdaExpression (lambdaParameters ( pars )) -> 
      //    (lambdaBody (block { stats }))
      // Assume 1 variable. 

      ASTTerm pars = (ASTTerm) terms.get(0); 
      String parString = pars.queryForm(); 
      ASTTerm body = (ASTTerm) terms.get(2); 
      String bodyString = body.toKM3(); 
      
      if (body.expression != null) 
      { Type ltype = new Type("OclAny", null);  
        UnaryExpression letexp = 
          UnaryExpression.newUnaryExpression("lambda", body.expression); 
        letexp.accumulator = new Attribute(parString, ltype, ModelElement.INTERNAL); 
        expression = letexp; 
        return letexp + ""; 
      } 

      if (body.statement != null) 
      { Type ltype = new Type("OclAny", null);  
        Attribute letvar = new Attribute(parString, ltype, ModelElement.INTERNAL); 
        UnaryExpression letexp = 
          UnaryExpression.newUnaryExpression("lambda", 
            letvar, body.statement, ASTTerm.currentClass); 
        letexp.accumulator = letvar; 
        expression = letexp; 
        return letexp + ""; 
      } 

      return "lambda " + parString + " in " + bodyString; 
    }     

    if ("lambdaBody".equals(tag))
    { ASTTerm stat = (ASTTerm) terms.get(0); 
      String res = stat.toKM3();
      expression = stat.expression;  
      statement = stat.statement; 
      return res; 
    } 

    if ("expression".equals(tag))
    { System.out.println(">> Expression with " + terms.size() + " terms " + terms);
      for (int y = 0; y < terms.size(); y++)
      { ASTTerm yt = (ASTTerm) terms.get(y); 
        System.out.println(">>> Term " + y + ": " + yt); 
      }  
      System.out.println(); 

      if (terms.size() == 1) // Identifier or literal
      { 
        ASTTerm t = (ASTTerm) terms.get(0); 
        String res = t.toKM3();
        System.out.println(">>> Single-argument expression: " + this + " with term: " + t + " |--> " + res + " expression: " + t.expression); 
        System.out.println(); 
        ASTTerm.setType(this,ASTTerm.getType(t));
        expression = t.expression;  
        statement = t.statement; 
        return res;          
      } 

      if (terms.size() == 2) // UnaryExpression
      { ASTTerm op = (ASTTerm) terms.get(0); 
        ASTTerm arg = (ASTTerm) terms.get(1);

        if ("new".equals(op + ""))
        { String res = arg.toKM3(); 
          ASTTerm.setType(this,ASTTerm.getType(arg));
          expression = arg.expression; 
          return res;  
        } 

        String op1 = op.toKM3(); 
        String arg1 = arg.toKM3(); 

        if ("-".equals(op.literalForm()))
        { ASTTerm.setType(this,ASTTerm.getType(arg)); 
          if (arg.expression != null) 
          { expression = 
              new UnaryExpression("-", arg.expression); 
          } 
          return "-" + arg1; 
        } 

        if ("+".equals(op.literalForm()))
        { ASTTerm.setType(this,ASTTerm.getType(arg));
          expression = arg.expression;  
          return arg1; 
        } 

        if ("++".equals(arg.literalForm()))
        { ASTTerm.setType(this,ASTTerm.getType(arg));

          if (op.expression != null) 
          { BasicExpression unit = new BasicExpression(1); 
            expression = new BinaryExpression("+", op.expression, unit); 
            
            statement = new AssignStatement(op.expression, expression); 
          } 

          System.out.println(">> Query form of " + this + " is: " + expression); 
          System.out.println(">> Update form of " + this + " is: " + statement); 
          System.out.println(); 

          return op1 + " := " + op1 + " + 1"; 
        } 
 
        if ("--".equals(arg.literalForm()))
        { ASTTerm.setType(this,ASTTerm.getType(arg)); 
          
          if (op.expression != null) 
          { Expression unit = new BasicExpression(1); 
            expression = new BinaryExpression("-", op.expression, unit); 
            statement = new AssignStatement(op.expression, expression); 
          } 

          return op1 + " := " + op1 + " - 1"; 
        }
 
        if ("++".equals(op.literalForm()))
        { ASTTerm.setType(this,ASTTerm.getType(arg));
          if (arg.expression != null) 
          { Expression unit = new BasicExpression(1); 
            expression = new BinaryExpression("+", arg.expression, unit); 
            statement = new AssignStatement(arg.expression, expression); 
          } 
         
          return arg1 + " := " + arg1 + " + 1"; 
        }
 
        if ("--".equals(op.literalForm()))
        { ASTTerm.setType(this,ASTTerm.getType(arg)); 
          if (arg.expression != null) 
          { Expression unit = new BasicExpression(1); 
            expression = new BinaryExpression("-", arg.expression, unit); 
            statement = new AssignStatement(arg.expression, expression); 
          } 
          return arg1 + " := " + arg1 + " - 1"; 
        }
 
        if (arg.expression != null) 
        { expression = UnaryExpression.newUnaryExpression(op1, arg.expression); }   

        System.out.println(">> Query form of " + this + " is: " + expression); 
        System.out.println(">> Update form of " + this + " is: " + statement); 
        System.out.println(); 
        
        return op1 + arg1; 
      }  



      if (terms.size() == 3) // BinaryExpression
      { ASTTerm op = (ASTTerm) terms.get(1); 
        ASTTerm e1 = (ASTTerm) terms.get(0);
        ASTTerm e2 = (ASTTerm) terms.get(2);
        String opx = op.toKM3(); 
        String e1x = e1.toKM3(); 
        String e2x = e2.toKM3();
        String e1literal = e1.literalForm(); 
 
        if ("PI".equals(e2 + "") && "Math".equals(e1literal))
        { ASTTerm.setType(this,"double"); 
          expression = new BasicExpression(3.141592653589793); 
          return "3.141592653589793"; 
        }
 
        if ("E".equals(e2 + "") && "Math".equals(e1literal))
        { ASTTerm.setType(this,"double"); 
          expression = new BasicExpression(2.718281828459045); 
          return "2.718281828459045"; 
        } 

        if ("TRUE".equals(e2 + "") && "Boolean".equals(e1literal))
        { ASTTerm.setType(this, "boolean"); 
          expression = new BasicExpression(true); 
          return "true"; 
        }
 
        if ("FALSE".equals(e2 + "") && "Boolean".equals(e1literal))
        { ASTTerm.setType(this, "boolean"); 
          expression = new BasicExpression(false); 
          return "false"; 
        }
 
        if ("TYPE".equals(e2 + "") && "Boolean".equals(e1literal))
        { ASTTerm.setType(this, "OclType"); 
          BasicExpression be = new BasicExpression(true);
          expression = new UnaryExpression("->oclType", be);  
          return "true->oclType()"; 
        }

        if ("boolean".equals(e1literal) && 
            "class".equals(e2 + ""))
        { ASTTerm.setType(this, "OclType"); 
          BasicExpression be = new BasicExpression(true);
          expression = new UnaryExpression("->oclType", be);  
          return "true->oclType()"; 
        }

        if ("NaN".equals(e2 + ""))
        { ASTTerm.setType(this, "double"); 
          expression = 
            BasicExpression.newValueBasicExpression(
                                           "Math_NaN");
          expression.setType(new Type("double", null));  
          return "Math_NaN"; 
        }
 
        if ("POSITIVE_INFINITY".equals(e2 + ""))
        { ASTTerm.setType(this, "double"); 
          expression = 
            BasicExpression.newValueBasicExpression(
                                     "Math_PINFINITY");
          expression.setType(new Type("double", null));  
          return "Math_PINFINITY"; 
        }
 
        if ("NEGATIVE_INFINITY".equals(e2 + ""))
        { ASTTerm.setType(this, "double"); 
          expression = 
            BasicExpression.newValueBasicExpression(
                                     "Math_NINFINITY");
          expression.setType(new Type("double", null));  
          return "Math_NINFINITY"; 
        } 

        if ("TYPE".equals(e2 + "") && "Void".equals(e1literal))
        { ASTTerm.setType(this, "OclType"); 
          expression = new BasicExpression(new Type("Void",null));
          return "OclType[\"void\"]"; 
        }

        if ("void".equals(e1literal) && 
            "class".equals(e2 + ""))
        { ASTTerm.setType(this, "OclType"); 
          expression = new BasicExpression(new Type("Void",null));
          return "OclType[\"void\"]"; 
        }

        if ("MIN_VALUE".equals(e2 + "") && "Byte".equals(e1literal))
        { ASTTerm.setType(this, "int"); 
          expression = new BasicExpression(-128); 
          return "-128"; 
        }
 
        if ("MAX_VALUE".equals(e2 + "") && "Byte".equals(e1literal))
        { ASTTerm.setType(this, "int"); 
          expression = new BasicExpression(127); 
          return "127"; 
        }
 
        if ("SIZE".equals(e2 + "") && "Byte".equals(e1literal))
        { ASTTerm.setType(this, "int"); 
          expression = new BasicExpression(8); 
          return "8"; 
        }
 
        if ("TYPE".equals(e2 + "") && "Byte".equals(e1literal))
        { ASTTerm.setType(this, "OclType"); 
          BasicExpression be = new BasicExpression(0);
          expression = new UnaryExpression("->oclType", be);  
          return "0->oclType()"; 
        }

        if ("class".equals(e2 + "") && "byte".equals(e1literal))
        { ASTTerm.setType(this, "OclType"); 
          BasicExpression be = new BasicExpression(0);
          expression = new UnaryExpression("->oclType", be);  
          return "0->oclType()"; 
        }

        if ("MIN_VALUE".equals(e2 + "") && "Character".equals(e1literal))
        { ASTTerm.setType(this, "String"); 
          expression = new BasicExpression("\"\\u0000\""); 
          expression.setType(new Type("String", null)); 

          return "\"\\u0000\""; 
        }
 
        if ("MAX_VALUE".equals(e2 + "") && "Character".equals(e1literal))
        { ASTTerm.setType(this, "String"); 
          expression = new BasicExpression("\"\\uFFFF\""); 
          expression.setType(new Type("String", null)); 
          return "\"\\uFFFF\""; 
        }
 
        if ("TYPE".equals(e2 + "") && "Character".equals(e1literal))
        { ASTTerm.setType(this, "OclType"); 
          BasicExpression be = new BasicExpression("\"\"");
          be.setType(new Type("String", null)); 
          expression = new UnaryExpression("->oclType", be);  
          return "\"\"->oclType()"; 
        }

        if ("class".equals(e2 + "") && "char".equals(e1literal))
        { ASTTerm.setType(this, "OclType"); 
          BasicExpression be = new BasicExpression("\"\"");
          be.setType(new Type("String", null)); 
          expression = new UnaryExpression("->oclType", be);  
          return "\"\"->oclType()"; 
        }

        if ("MIN_VALUE".equals(e2 + "") && "Short".equals(e1literal))
        { ASTTerm.setType(this, "int"); 
          expression = new BasicExpression(-32768); 
          return "-(2->pow(15))"; 
        }
 
        if ("MAX_VALUE".equals(e2 + "") && "Short".equals(e1literal))
        { ASTTerm.setType(this, "int"); 
          expression = new BasicExpression(32767); 
          return "(2->pow(15) - 1)"; 
        }
 
        if ("SIZE".equals(e2 + "") && "Short".equals(e1literal))
        { ASTTerm.setType(this, "int"); 
          expression = new BasicExpression(16); 
          return "16"; 
        }
  
        if ("TYPE".equals(e2 + "") && "Short".equals(e1literal))
        { ASTTerm.setType(this, "OclType"); 
          BasicExpression be = new BasicExpression(0);
          expression = new UnaryExpression("->oclType", be);  
          return "0->oclType()"; 
        }

        if ("class".equals(e2 + "") && "short".equals(e1literal))
        { ASTTerm.setType(this, "OclType"); 
          BasicExpression be = new BasicExpression(0);
          expression = new UnaryExpression("->oclType", be);  
          return "0->oclType()"; 
        }

        if ("MIN_VALUE".equals(e2 + "") && "Float".equals(e1literal))
        { ASTTerm.setType(this, "double"); 
          expression = new BasicExpression(1.401298464324817E-45); 
          return "(2->pow(-149))"; 
        }
 
        if ("MAX_VALUE".equals(e2 + "") && "Float".equals(e1literal))
        { ASTTerm.setType(this, "double"); 
          expression = new BasicExpression(3.4028234663852886E38); 
          return "3.4028234663852886*(10->pow(38))"; 
        }
 
        if ("SIZE".equals(e2 + "") && "Float".equals(e1literal))
        { ASTTerm.setType(this, "int"); 
          expression = new BasicExpression(32); 
          return "32"; 
        }
 
        if ("TYPE".equals(e2 + "") && "Float".equals(e1literal))
        { ASTTerm.setType(this, "OclType"); 
          BasicExpression be = new BasicExpression(0.0);
          expression = new UnaryExpression("->oclType", be);  
          return "(0.0)->oclType()"; 
        }

        if ("class".equals(e2 + "") && "float".equals(e1literal))
        { ASTTerm.setType(this, "OclType"); 
          BasicExpression be = new BasicExpression(0.0);
          expression = new UnaryExpression("->oclType", be);  
          return "(0.0)->oclType()"; 
        }

        if ("MIN_VALUE".equals(e2 + "") && "Double".equals(e1literal))
        { ASTTerm.setType(this, "double"); 
          expression = new BasicExpression(4.9E-324); 
          return "2->pow(-1074)"; 
        }
 
        if ("MAX_VALUE".equals(e2 + "") && "Double".equals(e1literal))
        { ASTTerm.setType(this, "double"); 
          expression = new BasicExpression(1.7976931348623157E308); 

          return "(2 - 2->pow(-52))*(2->pow(1023))"; 
        }
 
        if ("SIZE".equals(e2 + "") && "Double".equals(e1literal))
        { ASTTerm.setType(this, "int"); 
          expression = new BasicExpression(64); 
          return "64"; 
        }
 
        if ("TYPE".equals(e2 + "") && "Double".equals(e1literal))
        { ASTTerm.setType(this, "OclType"); 
          BasicExpression be = new BasicExpression(0.0);
          expression = new UnaryExpression("->oclType", be);  
          return "(0.0)->oclType()"; 
        }

        if ("class".equals(e2 + "") && "double".equals(e1literal))
        { ASTTerm.setType(this, "OclType"); 
          BasicExpression be = new BasicExpression(0.0);
          expression = new UnaryExpression("->oclType", be);  
          return "(0.0)->oclType()"; 
        }

        if ("EMPTY_LIST".equals(e2 + "") && "Collections".equals(e1x))
        { ASTTerm.setType(this, "Sequence"); 
          expression = new SetExpression(true);  
          return "Sequence{}"; 
        }
  
        if ("EMPTY_SET".equals(e2 + "") && "Collections".equals(e1x))
        { ASTTerm.setType(this, "Set"); 
          expression = new SetExpression();  
          return "Set{}"; 
        }
  
        if ("EMPTY_MAP".equals(e2 + "") && "Collections".equals(e1x))
        { ASTTerm.setType(this, "Map"); 
          expression = SetExpression.newMapSetExpression();  
          return "Map{}"; 
        }  

        if ("Integer".equals(e1literal))
        { ASTTerm.setType(this, "int"); 
          if ("MAX_VALUE".equals(e2 + ""))
          { expression = new BasicExpression(2147483647); 
            return "2147483647"; 
          } 
          else if ("MIN_VALUE".equals(e2 + ""))
          { expression = new BasicExpression(-2147483648); 
            return "-2147483648"; 
          } 
          else if ("SIZE".equals(e2 + ""))
          { expression = new BasicExpression(32); 
            return "32"; 
          } 
          else if ("TYPE".equals(e2 + ""))
          { BasicExpression be = new BasicExpression(0);
            expression = new UnaryExpression("->oclType", be);  
            ASTTerm.setType(this, "OclType"); 
            return "0->oclType()"; 
          }
        } 

        if ("int".equals(e1literal) && 
            "class".equals(e2 + ""))
        { BasicExpression be = new BasicExpression(0);
          expression = new UnaryExpression("->oclType", be);  
          ASTTerm.setType(this, "OclType"); 
          return "0->oclType()"; 
        }

        if ("Long".equals(e1literal))
        { ASTTerm.setType(this, "long"); 
          if ("MAX_VALUE".equals(e2 + ""))
          { expression = 
              new BasicExpression(9223372036854775807L); 
            return "9223372036854775807"; 
          } 
          else if ("MIN_VALUE".equals(e2 + ""))
          { expression = 
              new BasicExpression(-9223372036854775808L); 
            return "-9223372036854775808"; 
          } 
          else if ("SIZE".equals(e2 + ""))
          { expression = new BasicExpression(64); 
            ASTTerm.setType(this, "int"); 
            return "64"; 
          } 
          else if ("TYPE".equals(e2 + ""))
          // { return "9223372036854775807->oclType()"; }
          { expression = new BasicExpression(new Type("long", null)); 
            ASTTerm.setType(this, "OclType"); 
            return "OclType[\"long\"]"; 
          }
        } 

        if ("long".equals(e1literal) && 
            "class".equals(e2 + ""))
        { expression = 
            new BasicExpression(new Type("long", null)); 
          ASTTerm.setType(this, "OclType"); 
          return "OclType[\"long\"]"; 
        }

        if ("Thread".equals(e1literal))
        { ASTTerm.setType(this, "int"); 
          if ("MAX_PRIORITY".equals(e2 + ""))
          { expression = new BasicExpression(10); 
            return "10"; 
          } 
          else if ("MIN_PRIORITY".equals(e2 + ""))
          { expression = new BasicExpression(1); 
            return "1"; 
          } 
          else if ("NORM_PRIORITY".equals(e2 + ""))
          { expression = new BasicExpression(5); 
            return "5"; 
          } 
        } 


        if ("ONE".equals(e2 + "") && "BigInteger".equals(e1literal))
        { expression = new BasicExpression(1); 
          ASTTerm.setType(this, "int"); 
          return "1"; 
        } 
        
        if ("ZERO".equals(e2 + "") && "BigInteger".equals(e1literal))
        { expression = new BasicExpression(0); 
          ASTTerm.setType(this, "int"); 
          return "0"; 
        }
 
        if ("TEN".equals(e2 + "") && "BigInteger".equals(e1literal))
        { expression = new BasicExpression(10); 
          ASTTerm.setType(this, "int"); 
          return "10"; 
        } 

        if ("ONE".equals(e2 + "") && "BigDecimal".equals(e1literal))
        { expression = new BasicExpression(1.0); 
          ASTTerm.setType(this, "double"); 
          return "1.0"; 
        }
 
        if ("ZERO".equals(e2 + "") && "BigDecimal".equals(e1literal))
        { ASTTerm.setType(this, "double"); 
          expression = new BasicExpression(0.0);   
          return "0.0"; 
        }
 
        if ("TEN".equals(e2 + "") && "BigDecimal".equals(e1literal))
        { ASTTerm.setType(this, "double"); 
          expression = new BasicExpression(10.0);   
          return "10.0"; 
        } 

        if ("length".equals(e2 + "") || "elementCount".equals(e2 + ""))
        { ASTTerm.setType(this, "int"); 
          if (e1.expression != null) 
          { expression = new UnaryExpression("->size", e1.expression);            
            expression.setType(new Type("int", null)); 
          }   

          return e1x + "->size()"; 
        } 

        if ("=".equals(op + "") && e2.updatesObject(null))
        { // e1x := result of e2x ; sideeffect of e2x
          Statement updateF = e2.statement; 
          String qf = e2.queryForm(); 

          if (e1.expression != null && e2.expression != null) 
          { AssignStatement stat = 
              new AssignStatement(e1.expression, e2.expression);

            if (updateF == null) 
            { updateF = e2.statement; } 

            if (updateF != null) 
            { statement = new SequenceStatement(); 
              ((SequenceStatement) statement).addStatement(stat); 
              ((SequenceStatement) statement).addStatement(updateF); 
            } 
            else 
            { statement = stat; } 
          }
 
          return "(" + e1x + " := " + qf + " ; " + e2x + ")"; 
        } 

        if ("=".equals(op + "") && e2.hasSideEffect())
        { // pre sideeffect of e2 ; 
          // e1x := result of e2 ; 
          // post sideeffect of e2 -- the e2.toKM3()

          System.out.println(">>> Expression with side effect: " + this); 

          String prese = e2.preSideEffect();
          Statement preStat = e2.statement; 
          String postse = e2.postSideEffect(); 
          Statement postStat = e2.statement;
            
          if (prese == null && postse != null) 
          { String qf = e2.queryForm();  
            
            String res = "    " + e1x + " := " + 
                         qf + " ;\n    " + postse;
            Expression queryExp = e2.expression; 
            Expression lhs = e1.expression; 
            if (postStat != null && queryExp != null && 
                lhs != null) 
            { SequenceStatement stat = 
                new SequenceStatement(); 
              if ((lhs + "").equals(queryExp + "")) { } 
              else 
              { stat.addStatement(
                  new AssignStatement(lhs,queryExp)); 
              } 
              stat.addStatement(postStat); 
              statement = stat; 
            }  
            return res; 
          } 
          else if (postse == null) 
          { String qf = e2.queryForm();  

            Expression queryExp = e2.expression; 
            Expression lhs = e1.expression; 
            if (preStat != null && queryExp != null && 
                lhs != null) 
            { SequenceStatement stat = 
                new SequenceStatement(); 
              stat.addStatement(preStat); 
              if ((lhs + "").equals(queryExp + "")) { } 
              else 
              { stat.addStatement(
                  new AssignStatement(lhs,queryExp)); 
              } 
              statement = stat; 
            }  

            return prese + " ;" +  
                   "\n    " + e1x + " := " + qf; 
          } 
          else 
          { String qf = e2.queryForm();  

            Expression queryExp = e2.expression; 
            Expression lhs = e1.expression; 
            if (preStat != null && 
                postStat != null && queryExp != null && 
                lhs != null) 
            { SequenceStatement stat = 
                new SequenceStatement();
              stat.addStatement(preStat);  
              if ((lhs + "").equals(queryExp + "")) { } 
              else 
              { stat.addStatement(
                  new AssignStatement(lhs,queryExp)); 
              } 
              stat.addStatement(postStat); 
              statement = stat; 
            }  

            return prese + " ;" +  
                   "\n    " + e1x + " := " + qf + " ;\n    " + postse; 
          }  
        } 

        if ("=".equals(op + ""))
        { if (e1.expression != null && e2.expression != null) 
          { statement = 
              new AssignStatement(e1.expression, e2.expression);
            System.out.println(">> Assignment: " + statement);
            System.out.println();  
          } 

          return e1x + " := " + e2x; 
        } 
   
        if ("+=".equals(op + "") && e2.hasSideEffect())
        { // pre sideeffect of e2 ; 
          // e1x := e1x + (query form of e2) ; 
          // post sideeffect of e2 -- the e2.toKM3()

          System.out.println(">>> Expression with side effect: " + this); 

          String prese = e2.preSideEffect();
          Statement preStat = e2.statement; 
          String postse = e2.postSideEffect(); 
          Statement postStat = e2.statement;
            
          if (prese == null && postse != null) 
          { String qf = e2.queryForm(); 
 
            String res = 
              "    " + e1x + " := " + 
              e1x + " + " + qf + " ;\n    " + postse;
            Expression queryExp = e2.expression; 
            Expression lhs = e1.expression; 
            if (postStat != null && queryExp != null && 
                lhs != null) 
            { SequenceStatement stat = 
                new SequenceStatement(); 
              expression = 
                new BinaryExpression("+", e1.expression, 
                                     queryExp); 
              stat.addStatement(new AssignStatement(lhs,expression)); 
              stat.addStatement(postStat); 
              statement = stat; 
            }  
            return res; 
          } 
          else if (postse == null) 
          { String qf = e2.queryForm(); 
 
            Expression queryExp = e2.expression; 
            Expression lhs = e1.expression; 
            if (preStat != null && queryExp != null && 
                lhs != null) 
            { SequenceStatement stat = 
                new SequenceStatement(); 
              expression = 
                new BinaryExpression("+", e1.expression, 
                                     queryExp); 
              stat.addStatement(preStat); 
              stat.addStatement(
                new AssignStatement(lhs,expression)); 
              statement = stat; 
            }  
            
            return prese + " ;" +  
                   "\n    " + e1x + " := " + e1x + " + " + qf; 
          } 
          else // both non-null
          { String qf = e2.queryForm();

            Expression queryExp = e2.expression; 
            Expression lhs = e1.expression; 
            if (preStat != null && queryExp != null && 
                lhs != null && postStat != null) 
            { SequenceStatement stat = 
                new SequenceStatement(); 
              expression = 
                new BinaryExpression("+", e1.expression, 
                                     queryExp); 
              stat.addStatement(preStat); 
              stat.addStatement(
                new AssignStatement(lhs,expression));
              stat.addStatement(postStat);  
              statement = stat; 
            }  
 
            return prese + " ;" +  
              "\n    " + e1x + " := " + 
              e1x + " + " + qf + " ;\n    " + postse; 
          }  
        } 
     
        if ("+=".equals(op + ""))
        { if (e1.expression != null && e2.expression != null) 
          { expression = 
              new BinaryExpression("+", e1.expression, e2.expression); 
            statement = 
              new AssignStatement(e1.expression, expression);
            System.out.println(">> Assignment: " + statement);
            System.out.println();  
          } 
          return e1x + " := " + e1x + " + " + e2x;
        }

        if ("-=".equals(op + "") && e2.hasSideEffect())
        { // pre sideeffect of e2 ; 
          // e1x := e1x - (query form of e2) ; 
          // post sideeffect of e2 -- the e2.toKM3()

          System.out.println(">>> Expression with side effect: " + this); 

          String prese = e2.preSideEffect();
          Statement preStat = e2.statement; 
          String postse = e2.postSideEffect(); 
          Statement postStat = e2.statement;
            
          if (prese == null && postse != null) 
          { String qf = e2.queryForm(); 
 
            String res = 
              "    " + e1x + " := " + 
              e1x + " - (" + qf + ") ;\n    " + postse;

            Expression queryExp = e2.expression; 
            Expression lhs = e1.expression; 
            if (postStat != null && queryExp != null && 
                lhs != null) 
            { SequenceStatement stat = 
                new SequenceStatement();
              queryExp.setBrackets(true);  
              expression = 
                new BinaryExpression("-", e1.expression, 
                                     queryExp); 
              stat.addStatement(new AssignStatement(lhs,expression)); 
              stat.addStatement(postStat); 
              statement = stat; 
            }  
            return res; 
          } 
          else if (postse == null) 
          { String qf = e2.queryForm(); 
 
            Expression queryExp = e2.expression; 
            queryExp.setBrackets(true);  
            Expression lhs = e1.expression; 
            if (preStat != null && queryExp != null && 
                lhs != null) 
            { SequenceStatement stat = 
                new SequenceStatement(); 
              expression = 
                new BinaryExpression("-", e1.expression, 
                                     queryExp); 
              stat.addStatement(preStat); 
              stat.addStatement(
                new AssignStatement(lhs,expression)); 
              statement = stat; 
            }  
            
            return prese + " ;" +  
                   "\n    " + e1x + " := " + e1x + " - (" + qf + ")"; 
          } 
          else // both non-null
          { String qf = e2.queryForm();

            Expression queryExp = e2.expression; 
            queryExp.setBrackets(true);  

            Expression lhs = e1.expression; 
            if (preStat != null && queryExp != null && 
                lhs != null && postStat != null) 
            { SequenceStatement stat = 
                new SequenceStatement(); 
              expression = 
                new BinaryExpression("-", e1.expression, 
                                     queryExp); 
              stat.addStatement(preStat); 
              stat.addStatement(
                new AssignStatement(lhs,expression));
              stat.addStatement(postStat);  
              statement = stat; 
            }  
 
            return prese + " ;" +  
              "\n    " + e1x + " := " + 
              e1x + " - (" + qf + ") ;\n    " + postse; 
          }  
        } 

        if ("-=".equals(op + ""))
        { if (e1.expression != null && e2.expression != null) 
          { e2.expression.setBrackets(true); 
            expression = 
              new BinaryExpression("-", e1.expression, e2.expression); 
            statement = 
              new AssignStatement(e1.expression, expression);
            System.out.println(">> Assignment: " + statement);
            System.out.println();  
          } 
          return e1x + " := " + e1x + " - " + e2x;
        } 

        if ("*=".equals(op + "") && e2.hasSideEffect())
        { // pre sideeffect of e2 ; 
          // e1x := e1x * (query form of e2) ; 
          // post sideeffect of e2 -- the e2.toKM3()

          System.out.println(">>> Expression with side effect: " + this); 

          String prese = e2.preSideEffect();
          Statement preStat = e2.statement; 
          String postse = e2.postSideEffect(); 
          Statement postStat = e2.statement;
            
          if (prese == null && postse != null) 
          { String qf = e2.queryForm(); 
 
            String res = 
              "    " + e1x + " := " + 
              e1x + " * (" + qf + ") ;\n    " + postse;

            Expression queryExp = e2.expression; 
            Expression lhs = e1.expression; 
            if (postStat != null && queryExp != null && 
                lhs != null) 
            { SequenceStatement stat = 
                new SequenceStatement();
              queryExp.setBrackets(true);  
              expression = 
                new BinaryExpression("*", e1.expression, 
                                     queryExp); 
              stat.addStatement(new AssignStatement(lhs,expression)); 
              stat.addStatement(postStat); 
              statement = stat; 
            }  
            return res; 
          } 
          else if (postse == null) 
          { String qf = e2.queryForm(); 
 
            Expression queryExp = e2.expression; 
            queryExp.setBrackets(true);  
            Expression lhs = e1.expression; 
            if (preStat != null && queryExp != null && 
                lhs != null) 
            { SequenceStatement stat = 
                new SequenceStatement(); 
              expression = 
                new BinaryExpression("*", e1.expression, 
                                     queryExp); 
              stat.addStatement(preStat); 
              stat.addStatement(
                new AssignStatement(lhs,expression)); 
              statement = stat; 
            }  
            
            return prese + " ;" +  
                   "\n    " + e1x + " := " + e1x + " * (" + qf + ")"; 
          } 
          else // both non-null
          { String qf = e2.queryForm();

            Expression queryExp = e2.expression; 
            queryExp.setBrackets(true);  

            Expression lhs = e1.expression; 
            if (preStat != null && queryExp != null && 
                lhs != null && postStat != null) 
            { SequenceStatement stat = 
                new SequenceStatement(); 
              expression = 
                new BinaryExpression("*", e1.expression, 
                                     queryExp); 
              stat.addStatement(preStat); 
              stat.addStatement(
                new AssignStatement(lhs,expression));
              stat.addStatement(postStat);  
              statement = stat; 
            }  
 
            return prese + " ;" +  
              "\n    " + e1x + " := " + 
              e1x + " * (" + qf + ") ;\n    " + postse; 
          }  
        } 
 
        if ("*=".equals(op + ""))
        { if (e1.expression != null && e2.expression != null) 
          { e2.expression.setBrackets(true); 
            expression = 
              new BinaryExpression("*", e1.expression, 
                                   e2.expression); 
            statement = 
              new AssignStatement(e1.expression, expression);
            System.out.println(">> Assignment: " + statement);
            System.out.println();  
          } 
          return e1x + " := " + e1x + " * " + e2x; 
        } 

        if ("/=".equals(op + ""))
        { if (e1.expression != null && e2.expression != null) 
          { e2.expression.setBrackets(true); 
            expression = 
              new BinaryExpression("/", e1.expression, 
                                   e2.expression); 
            statement = 
              new AssignStatement(e1.expression, expression);
            System.out.println(">> Assignment: " + statement);
            System.out.println();  
          } 
          return e1x + " := " + e1x + " / " + e2x; 
        }
 
        if ("^=".equals(op + ""))
        { if (e1.expression != null && e2.expression != null) 
          { e2.expression.setBrackets(true); 
            expression = 
              new BinaryExpression("xor", e1.expression, 
                                   e2.expression); 
            statement = 
              new AssignStatement(e1.expression, expression);
            System.out.println(">> Assignment: " + statement);
            System.out.println();  
          } 
          return e1x + " := " + e1x + " xor " + e2x; 
        }
 
        if ("&=".equals(op + ""))
        { if (e1.expression != null && e2.expression != null) 
          { e2.expression.setBrackets(true); 
            expression = 
              new BinaryExpression("&", e1.expression, 
                                   e2.expression); 
            statement = 
              new AssignStatement(e1.expression, expression);
            System.out.println(">> Assignment: " + statement);
            System.out.println();  
          } 
          return e1x + " := " + e1x + " & " + e2x; 
        } 

        if ("|=".equals(op + ""))
        { if (e1.expression != null && e2.expression != null) 
          { e2.expression.setBrackets(true); 
            expression = 
              new BinaryExpression("or", e1.expression, 
                                   e2.expression); 
            statement = 
              new AssignStatement(e1.expression, expression);
            System.out.println(">> Assignment: " + statement);
            System.out.println();  
          } 
          return e1x + " := " + e1x + " or " + e2x; 
        }
 
        if ("%=".equals(op + ""))
        { if (e1.expression != null && e2.expression != null) 
          { e2.expression.setBrackets(true); 
            expression = 
              new BinaryExpression("mod", e1.expression, 
                                   e2.expression); 
            statement = 
              new AssignStatement(e1.expression, expression);
            System.out.println(">> Assignment: " + statement);
            System.out.println();  
          } 
          return e1x + " := " + e1x + " mod " + e2x; 
        } 
        
        if (".".equals(op + ""))
        { return featureAccess(e1,e2,e1x,e2x); } 

        if ("<<".equals(op + ""))
        { ASTTerm.setType(this, "long"); 
          return e1x + "*(2->pow(" + e2x + "))"; 
        }
 
        if (">>".equals(op + "") || ">>>".equals(op + ""))
        { ASTTerm.setType(this, "long"); 
          return e1x + "/(2->pow(" + e2x + "))"; 
        } 

        if ("instanceof".equals(op + ""))
        { ASTTerm.setType(this, "boolean"); 
          if (e1.expression != null && e2.expression != null) 
          { expression = new BinaryExpression("->oclIsKindOf", e1.expression, e2.expression);
            expression.setType(new Type("boolean", null)); 
          } 

          System.out.println(">> instanceof expression: " + expression); 

          return e1x + "->oclIsKindOf(" + e2x + ")"; 
        } 

        // Any custom translation for binary infix operators 
        // goes here. 

        if ("%".equals(op.literalForm()) || 
            "mod".equals(opx))
        { ASTTerm.setType(this, "int");
          // System.out.println(">>> Type of " + this + " is int"); 
          // System.out.println();
          if (e1.expression != null && e2.expression != null) 
          { expression = 
              new BinaryExpression("mod", e1.expression, 
                                          e2.expression); 
            expression.setType(new Type("int", null)); 
          }  
          return e1x + " mod " + e2x;  
        }

        if ("/".equals(op.literalForm()) || 
            "*".equals(op.literalForm()) ||
            "-".equals(op.literalForm()))
        { if (e1.isString() && e2.isString())
          { // 'z' - 'a' etc
            ASTTerm.setType(this, "int"); 

            if (e1.expression != null) 
            { e1.expression = 
               new UnaryExpression("->char2byte", e1.expression); 
            } 
            e1x = "(" + e1x + ")->char2byte()"; 

            if (e2.expression != null) 
            { e2.expression = 
               new UnaryExpression("->char2byte", e2.expression); 
            } 
            e2x = "(" + e2x + ")->char2byte()"; 
          } 
          else if (e1.isString() && e2.isInteger())
          { // 'a' - 97 etc
            ASTTerm.setType(this, "int"); 
            if (e1.expression != null) 
            { e1.expression = 
               new UnaryExpression("->char2byte", e1.expression); 
            } 
            e1x = "(" + e1x + ")->char2byte()"; 
          } 
          else if (e1.isInteger() && e2.isString())
          { // 'a' - 97 etc
            ASTTerm.setType(this, "int"); 
            if (e2.expression != null) 
            { e2.expression = 
               new UnaryExpression("->char2byte", e2.expression); 
            } 
            e2x = "(" + e2x + ")->char2byte()"; 
          } 
          else if (e1.isInteger() && e2.isInteger())
          { ASTTerm.setType(this, ASTTerm.getType(e1)); } 
          else 
          { ASTTerm.setType(this, "double"); }  

          if (e1.expression != null && e2.expression != null) 
          { expression = 
              new BinaryExpression(op.literalForm(), e1.expression, 
                                          e2.expression); 
          }  

          return e1x + " " + op.literalForm() + " " + e2x;  
        }

        if ("+".equals(op.literalForm()))
        { if (e1.isInteger() && e2.isInteger())
          { ASTTerm.setType(this, ASTTerm.getType(e1)); } 
          else if (e1.isString() || e2.isString())
          { ASTTerm.setType(this, "String"); }
          else 
          { ASTTerm.setType(this, "double"); }

          if (e1.expression != null && e2.expression != null) 
          { expression = 
              new BinaryExpression("+", e1.expression, 
                                        e2.expression); 
          }  

          return e1x + " + " + e2x;  
        }

        if ("==".equals(op.literalForm()))
        { ASTTerm.setType(this, "boolean");
          String e1xx = e1.toKM3(); 
          String e2xx = e2.toKM3(); 
          if (e1.isIdentifier() && e2.isIdentifier()  && 
              (e1.isCollection() || e2.isCollection()))
          { if (e1.expression != null && e2.expression != null) 
            { UnaryExpression ref1 = new UnaryExpression("?", e1.expression); 
              UnaryExpression ref2 = new UnaryExpression("?", e2.expression); 
              expression = new BinaryExpression("=", ref1, ref2); 
              expression.setType(new Type("boolean", null)); 
            } 
            return "?" + e1xx + " = ?" + e2xx; 
          } 

          if (e1.expression != null && e2.expression != null) 
          { expression = new BinaryExpression("=", e1.expression, e2.expression); 
            expression.setType(new Type("boolean", null)); 
          } 

          return e1xx + " = " + e2xx; 
        }

        if ("!=".equals(op.literalForm()))
        { ASTTerm.setType(this, "boolean");
          String e1xx = e1.toKM3(); 
          String e2xx = e2.toKM3(); 
          if (e1.isIdentifier() && e2.isIdentifier()  && 
              (e1.isCollection() || e2.isCollection()))
          { if (e1.expression != null && e2.expression != null) 
            { UnaryExpression ref1 = new UnaryExpression("?", e1.expression); 
              UnaryExpression ref2 = new UnaryExpression("?", e2.expression); 
              expression = new BinaryExpression("/=", ref1, ref2); 
              expression.setType(new Type("boolean", null)); 
            } 
            return "?" + e1xx + " /= ?" + e2xx; 
          } 

          if (e1.expression != null && e2.expression != null) 
          { expression = new BinaryExpression("/=", e1.expression, e2.expression); 
            expression.setType(new Type("boolean", null)); 
          } 

          return e1xx + " /= " + e2xx; 
        }

        if ("&".equals(op + "") && e1.isInteger() && 
            e2.isInteger())
        { ASTTerm.setType(this, "int");

          if (e1.expression != null && 
              e2.expression != null) 
          { Vector parms = new Vector(); 
            parms.add(e1.expression); 
            parms.add(e2.expression); 
            expression = BasicExpression.newStaticCallBasicExpression("bitwiseAnd", "MathLib", parms);
            expression.setType(new Type("int", null)); 
          } 

          return "MathLib.bitwiseAnd(" + e1x + ", " + e2x + ")"; 
        }
 
        if ("|".equals(op + "") && e1.isInteger() && 
            e2.isInteger())
        { ASTTerm.setType(this, "int");

          if (e1.expression != null && 
              e2.expression != null) 
          { Vector parms = new Vector(); 
            parms.add(e1.expression); 
            parms.add(e2.expression); 
            expression = BasicExpression.newStaticCallBasicExpression("bitwiseOr", "MathLib", parms); 
            expression.setType(new Type("int", null)); 
          } 

          return "MathLib.bitwiseOr(" + e1x + ", " + e2x + ")"; 
        } 


        if ("^".equals(op + "") && e1.isInteger() && 
            e2.isInteger())
        { ASTTerm.setType(this, "int");

          if (e1.expression != null && 
              e2.expression != null) 
          { Vector parms = new Vector(); 
            parms.add(e1.expression); 
            parms.add(e2.expression); 
            expression = BasicExpression.newStaticCallBasicExpression("bitwiseXor", "MathLib", parms); 
            expression.setType(new Type("int", null)); 
          } 

          return "MathLib.bitwiseXor(" + e1x + ", " + e2x + ")"; 
        } 


        if ("<".equals(op.literalForm()) ||
            ">".equals(op.literalForm()) || 
            ">=".equals(op.literalForm()) || 
            "==".equals(op.literalForm()) ||
            "<=".equals(op.literalForm()) ||
            "!=".equals(op.literalForm()) ||
            "&&".equals(op.literalForm()) ||
            "||".equals(op.literalForm()))
        { ASTTerm.setType(this, "boolean");
          System.out.println(">>> Type of " + this + " is boolean"); 
          System.out.println(); 
        }
          
        if (e1.expression != null && e2.expression != null) 
        { expression = 
            new BinaryExpression(opx.trim(), 
                  e1.expression, e2.expression); 

          if (Expression.isComparator(opx))
          { expression.setType(new Type("boolean", null)); }

          System.out.println(">>> Binary expression is: " + expression); 
          System.out.println(); 
        } 

        return e1x + opx + e2x; 
      }  

      if (terms.size() == 4 && 
          "(".equals(terms.get(0) + "") && 
          ")".equals(terms.get(2) + "")) // cast
      { ASTTerm typ = (ASTTerm) terms.get(1); 
        ASTTerm arg = (ASTTerm) terms.get(3);

        String typx = typ.toKM3(); 
        String argx = arg.toKM3();
        ASTTerm.setType(this, typx);

        if (arg.updatesObject(null))
        { return argx; }
  
        String qf = arg.queryForm(); 
        String ct = typx; 

        ASTTerm.setType(this, ct); 

        if ("String".equals(ASTTerm.getType(arg)) && 
            ("int".equals(ct) || "long".equals(ct) ||
             "short".equals(ct) || "byte".equals(ct))
           )
        { if (arg.expression != null)
          { arg.expression.setBrackets(true); 
            expression = 
              new UnaryExpression("->char2byte", 
                                  arg.expression); 
          }
          return "(" + qf + ")->char2byte()"; 
        }  

        if (arg.expression != null) 
        { Expression typexpr = new BasicExpression(new Type(typx, null)); 
          arg.expression.setBrackets(true); 
          expression = new BinaryExpression("->oclAsType", arg.expression, typexpr); 
        }

        return "(" + argx + ")->oclAsType(" + typx + ")";
      }  


      if (terms.size() == 4 && 
          "[".equals(terms.get(1) + "") && 
          "]".equals(terms.get(3) + "")) // array access
      { ASTTerm arr = (ASTTerm) terms.get(0); 
        ASTTerm ind = (ASTTerm) terms.get(2);

        String arrx = arr.toKM3(); 
        String indx = ind.toKM3();

        String typ = ASTTerm.getType(arr); 
        String elemType = ASTTerm.getElementType(arr); 
        ASTTerm.setType(this, elemType); 

        System.out.println(">>>--->>> element type of " + this + " type " + typ + " is " + elemType); 
        System.out.println(); 

        if (arr.expression != null && 
            ind.expression != null)
        { BasicExpression unit = new BasicExpression(1); 
          Expression inde = new BinaryExpression("+", ind.expression, unit);  

          expression = 
             BasicExpression.newIndexedBasicExpression(arr.expression, inde); 
        } 
        
        return arrx + "[" + indx + " + 1]";
      } // It must be indexed by integers. Not a map. 

      if (terms.size() == 4 && 
          "<".equals(terms.get(1) + "") &&
          "<".equals(terms.get(2) + ""))
      { ASTTerm.setType(this, "long"); 
        ASTTerm arg1 = (ASTTerm) terms.get(0); 
        ASTTerm arg2 = (ASTTerm) terms.get(3);

        String arg1x = arg1.toKM3(); 
        String arg2x = arg2.toKM3();

        if (arg1.expression != null && 
            arg2.expression != null)
        { BinaryExpression rhs = 
            new BinaryExpression("->pow", 
                        new BasicExpression(2),
                        arg2.expression); 
          expression = 
            new BinaryExpression("*", arg1.expression, rhs); 
        } 
 
        return "(" + arg1x + "*(2->pow(" + arg2x + ")))->oclAsType(long)"; 
      } 

      if (terms.size() == 4 && 
          ">".equals(terms.get(1) + "") &&
          ">".equals(terms.get(2) + ""))
      { ASTTerm.setType(this, "long"); 
        ASTTerm arg1 = (ASTTerm) terms.get(0); 
        ASTTerm arg2 = (ASTTerm) terms.get(3);

        String arg1x = arg1.toKM3(); 
        String arg2x = arg2.toKM3();

        if (arg1.expression != null && 
            arg2.expression != null)
        { BinaryExpression rhs = 
            new BinaryExpression("->pow", 
                        new BasicExpression(2),
                        arg2.expression); 
          expression = 
            new BinaryExpression("/", arg1.expression, rhs); 
        } 
        return "(" + arg1x + "/(2->pow(" + arg2x + ")))->oclAsType(long)"; 
      }

      if (terms.size() == 5 && "?".equals(terms.get(1) + ""))
      { // ConditionalExpression
        ASTTerm cond = (ASTTerm) terms.get(0); 
        ASTTerm ifoption = (ASTTerm) terms.get(2);
        ASTTerm elseoption = (ASTTerm) terms.get(4);
        String condx = cond.toKM3(); 
        String ifx = ifoption.toKM3(); 
        String elsex = elseoption.toKM3();
        ASTTerm.setType(this, ASTTerm.getType(ifoption)); 
          
        Expression ce = cond.expression; 
        Expression ife = ifoption.expression; 
        Expression elsee = elseoption.expression; 
        if (ce != null && ife != null && elsee != null) 
        { expression = 
            new ConditionalExpression(ce,ife,elsee); 
        }

        return "if " + condx + " then " + ifx + " else " + elsex + " endif"; 
      } 
    } 

    if ("statement".equals(tag))
    { System.out.println(">> Statement with " + terms.size() + " terms "); 

      for (int h = 0; h < terms.size(); h++) 
      { System.out.println("--- Term " + h + ": " + terms.get(h)); } 
      System.out.println(); 

      if (terms.size() >= 2 && "throw".equals(terms.get(0) + "")) 
      { ASTTerm t = (ASTTerm) terms.get(1);
        String trexp = t.toKM3(); 
        if (t.expression != null) 
        { statement = new ErrorStatement(t.expression); }
 
        return "  error " + t.toKM3(); 
      }
      else if (terms.size() > 2 && "if".equals(terms.get(0) + ""))
      { // (statement if expr stat1 else stat2) 
        // (statement if expr stat1)

        ASTTerm texpr = (ASTTerm) terms.get(1);
        String res = "    if " + texpr.toKM3() + " then ";
        if (terms.size() > 4 && 
            "else".equals(((ASTTerm) terms.get(3)).literalForm()))
        { ASTTerm ifPart = (ASTTerm) terms.get(2); 
          ASTTerm elsePart = (ASTTerm) terms.get(4);
          res = res + ifPart.toKM3() + "\n"; 
          res = res + "    else (\n"; 
          res = res + "      " + elsePart.toKM3() + "\n" + 
                      "    ) ";

          if (texpr.expression != null && 
              ifPart.statement != null && 
              elsePart.statement != null)
          { texpr.expression.setType(
              new Type("boolean", null)); 
            statement = 
              new ConditionalStatement(texpr.expression,
                       ifPart.statement, elsePart.statement); 
            elsePart.statement.setBrackets(true); 
          } 
 
          return res; 
        } 
  
        for (int i = 2; i < terms.size(); i++) 
        { ASTTerm tt = (ASTTerm) terms.get(i); 
          res = res + "  " + tt.toKM3();
        } 

        if (terms.size() == 3) // no else
        { res = res + "  else skip"; 

          ASTTerm ifPart = (ASTTerm) terms.get(2); 
          if (texpr.expression != null && 
              ifPart.statement != null)
          { InvocationStatement skip = new InvocationStatement("skip"); 
            texpr.expression.setType(
              new Type("boolean", null)); 
            statement = 
              new ConditionalStatement(texpr.expression,
                       ifPart.statement, skip); 
          } 
        } 

        return res; 
      } 
      else if (terms.size() > 2 && "try".equals(terms.get(0) + ""))
      { ASTTerm tbody = (ASTTerm) terms.get(1);
        String res = "  try " + tbody.toKM3() + "\n";

        if (tbody.statement != null) 
        { statement = new TryStatement(tbody.statement); } 
        else 
        { statement = new TryStatement(new SequenceStatement()); } 

        for (int i = 2; i < terms.size(); i++) 
        { ASTTerm tt = (ASTTerm) terms.get(i); 
          res = res + "    " + tt.toKM3();
          System.out.println(">>> Statement of " + tt + " is: " + tt.statement); 
          System.out.println(); 

          if (statement != null && tt.statement != null)
          { ((TryStatement) statement).addClause(tt.statement); } 
        } 
        return res; 
      } 
      else if (terms.size() > 2 && "switch".equals(terms.get(0) + ""))
      { // (statement switch expr 
        //  { (switchBlockStatementGroup switchLabel* blockStatement*)* }
 
        Vector stats = new Vector(); 

        ASTTerm ttest = (ASTTerm) terms.get(1);
        String etest = ttest.toKM3();
          
        String res = "  for _i : Integer.subrange(1,1) do (\n";
        res = res + caseStatementstoKM3(etest,ttest.expression,terms,stats); 
        res = res + "  )\n";

        BasicExpression ivar = new BasicExpression("_i"); 
        ivar.setType(new Type("int", null)); 
        BasicExpression unit = new BasicExpression(1); 
        Vector pars = new Vector(); 
        pars.add(unit); 
        pars.add(unit); 
        BasicExpression rng = 
          BasicExpression.newFunctionBasicExpression(
            "subrange", "Integer", pars);
        Type intseqType = new Type("Sequence", null);
        intseqType.setElementType(new Type("int", null)); 
        rng.setType(intseqType);  

        Expression tst = new BinaryExpression(":", ivar, rng); 
        tst.setType(new Type("boolean", null)); 

        Statement body = new SequenceStatement(stats); 
        body.setBrackets(true); 
        statement = new WhileStatement(tst,body);  
        ((WhileStatement) statement).setLoopKind(Statement.FOR); 
        ((WhileStatement) statement).setLoopRange(ivar,rng);
        
        return res; 
      } 
      else if (terms.size() > 2 && "while".equals(terms.get(0) + ""))
      { ASTTerm texpr = (ASTTerm) terms.get(1);
        String res = "  while " + texpr.toKM3() + 
                     "\n    do\n  ";


        SequenceStatement body = new SequenceStatement(); 
 
        for (int i = 2; i < terms.size(); i++) 
        { ASTTerm tt = (ASTTerm) terms.get(i); 
          String bstat = tt.toKM3(); 
          if (tt.statement != null) 
          { body.addStatement(tt.statement); } 

          res = res + "    " + bstat;
        } 

        body.setBrackets(true); 

        if (texpr.expression != null) 
        { texpr.expression.setType(
              new Type("boolean", null)); 
          statement = new WhileStatement(texpr.expression,body);
        } 

        System.out.println(">>*** While body: " + statement); 

        return res; 
      } 
      else if (terms.size() > 3 && "do".equals(terms.get(0) + ""))
      { ASTTerm stat = (ASTTerm) terms.get(1);
        String statcode = stat.toKM3();  
        ASTTerm texpr = (ASTTerm) terms.get(3);

        if (texpr.expression != null && stat.statement != null) 
        { texpr.expression.setType(
                   new Type("boolean", null)); 
          stat.statement.setBrackets(true); 
          Statement loop = new WhileStatement(texpr.expression,stat.statement); 
          statement = new SequenceStatement(); 
          ((SequenceStatement) statement).addStatement(stat.statement); 
          ((SequenceStatement) statement).addStatement(loop); 
        } 

        String res = "  " + statcode + " ;\n" + 
          "  while " + texpr.toKM3() + "\n   do\n" + 
          "    " + statcode; 
        return res; 
      } 
      else if (terms.size() > 2 && "for".equals(terms.get(0) + ""))
      { ASTCompositeTerm forControl = (ASTCompositeTerm) terms.get(2);
        ASTTerm forTst = forControl.forTest(); 
        ASTTerm forIni = forControl.forInit(); 
        ASTTerm forInc = forControl.forIncr(); 

        String tst = forControl.toKM3Test(); 
        String init = forControl.toKM3Init(); 
        String incr = forControl.toKM3Incr(); 
		
        String loopKind = "while"; 
        if (forControl.isEnhancedForControl())
        { loopKind = "for"; } 

        String res = "  "; 
        if (init != null) 
        {  res = res + init + " ;\n  "; } 
		
        SequenceStatement lBody = new SequenceStatement(); 

        res = res + "  " + loopKind + " " + tst + "\n    do\n    ( ";
		 
        for (int i = 4; i < terms.size(); i++) 
        { ASTTerm tt = (ASTTerm) terms.get(i); 
          res = res + "  " + tt.toKM3();
          if (tt.statement != null) 
          { lBody.addStatement(tt.statement); } 
        } // but could be empty. Also, any continue
          // must be preceded by incr, if you change
          // for to while.  
		
        if (incr == null) 
        { res = res + " )"; }
        else 
        { res = res + " ; \n    " + forControl.toKM3Incr() + "\n" + 
          "  )";
        }   

        if (forInc != null && forInc.statement != null) 
        { lBody.addStatement(forInc.statement); }

        lBody.setBrackets(true); 

        if (forIni != null && forIni.statement != null) 
        { statement = new SequenceStatement(); 
          ((SequenceStatement) statement).addStatement(forIni.statement); 
          if (loopKind.equals("while") &&
              forTst != null &&  
              forTst.expression != null) 
          { forTst.expression.setType(
              new Type("boolean", null)); 
            WhileStatement ws = 
              new WhileStatement(forTst.expression, lBody); 
            ((SequenceStatement) statement).addStatement(ws); 
          } 
          else if (loopKind.equals("for") && 
              forTst != null &&  
              forTst.expression != null) 
          { forTst.expression.setType(
                                new Type("boolean", null)); 
            WhileStatement ws = 
              new WhileStatement(forTst.expression, lBody);
            ws.setLoopKind(Statement.FOR);  
            ws.setLoopRange(forTst.expression);
            ((SequenceStatement) statement).addStatement(ws); 
          } 
        }  
        else 
        { if (loopKind.equals("while") && 
              forTst != null &&  
              forTst.expression != null) 
          { forTst.expression.setType(
              new Type("boolean", null)); 
            statement = 
              new WhileStatement(forTst.expression, lBody); 
          } 
          else if (loopKind.equals("for") && 
              forTst != null &&  
              forTst.expression != null) 
          { forTst.expression.setType(
                                   new Type("boolean", null)); 
            statement = 
              new WhileStatement(forTst.expression, lBody); 
            ((WhileStatement) statement).setLoopKind(Statement.FOR);  
            ((WhileStatement) statement).setLoopRange(forTst.expression);
          } 
        }  

        return res; 
      } 
      else if (terms.size() == 3 && "assert".equals(terms.get(0) + ""))
      { ASTTerm expr = (ASTTerm) terms.get(1);
        String res = "  assert " + expr.toKM3(); 
        if (expr.expression != null) 
        { statement = new AssertStatement(expr.expression); } 
        return res; 
      } 
      else if (terms.size() > 3 && "assert".equals(terms.get(0) + ""))
      { ASTTerm expr = (ASTTerm) terms.get(1);
        ASTTerm mess = (ASTTerm) terms.get(3);
        String res = "  assert " + expr.toKM3() + " do " + mess.toKM3(); 
        if (expr.expression != null && mess.expression != null) 
        { statement = 
            new AssertStatement(expr.expression, mess.expression); 
        } 
        return res; 
      } 
      else if (terms.size() > 2 && "return".equals(terms.get(0) + ""))
      { ASTTerm texpr = (ASTTerm) terms.get(1);
        String res = "  return " + texpr.toKM3() + " "; 

        if (texpr.hasSideEffect()) // preSide
        { String qf = texpr.queryForm(); 
          Statement stat2 = new ReturnStatement(texpr.expression); 
          String pse = texpr.preSideEffect(); 
          if (pse == null || texpr.statement == null) 
          { statement = stat2; 
            res = "    return " + qf + " "; 
          } 
          else // assume texpr.expression != null
          { statement = new SequenceStatement(); 
            ((SequenceStatement) statement).addStatement(texpr.statement); 
            ((SequenceStatement) statement).addStatement(stat2); 
            res = pse + " ;\n" + 
                  "    return " + qf + " ";
          } 
        } 
        else if (texpr.expression != null) 
        { statement = new ReturnStatement(texpr.expression); } 

        return res; 
      } 
      else if (terms.size() > 0 && "return".equals(terms.get(0) + ""))
      { statement = new ReturnStatement(); 

        return "  return "; 
      } 
      else if (terms.size() > 0 && "continue".equals(terms.get(0) + ""))
      { statement = new ContinueStatement(); 

        return "  continue "; 
      } 
      else if (terms.size() > 0 && "break".equals(terms.get(0) + ""))
      { statement = new BreakStatement(); 

        return "  break "; 
      }
      else if (terms.size() == 2) // Return, break, continue or expression statement
      { ASTTerm t = (ASTTerm) terms.get(0);
        if (t.isAssignment()) 
        { String res = "  " + t.toKM3Assignment();
          statement = t.statement;
          return res; 
        } 
        String rs = t.toKM3(); // update form of expression
        statement = t.statement;

        System.out.println(">>> Statement of " + rs + " is " + statement); 
        System.out.println();  
        return "  " + rs; 
      }

      if (terms.size() == 1) // Single statement
      { ASTTerm t = (ASTTerm) terms.get(0); 
        String res = t.toKM3();
        statement = t.statement; 
        return res;  
      }  
    }  

    if ("catchClause".equals(tag))
    { ASTTerm ctest = (ASTTerm) terms.get(2); 
      ASTTerm cvar = (ASTTerm) terms.get(3); 
      ASTTerm cbody = (ASTTerm) terms.get(5);

      String catchType = ctest.toKM3();
      String catchVar = cvar.toKM3(); 
      String catchBody = cbody.toKM3(); 
  
      if (ctest.expression != null && catchVar != null) 
      { BinaryExpression test = 
          new BinaryExpression(":",  
            BasicExpression.newVariableBasicExpression(
                                           catchVar),
            ctest.expression);
        if (cbody.statement != null) { } 
        else 
        { cbody.statement = new InvocationStatement("skip"); } 
        statement = new CatchStatement(test, cbody.statement);  
      } 

      return "  catch (" + catchVar + " : " + catchType + ") do " + catchBody + "\n"; 
    } 

    if ("catchType".equals(tag))
    { ASTTerm typ = (ASTTerm) terms.get(0); 
      String res = typ.toKM3(); 
      modelElement = typ.modelElement; 
      expression = typ.expression; 
      System.out.println(">>> Expression of " + this + " is " + expression); 

      return res; 
    } 

    if ("finallyBlock".equals(tag))
    { ASTTerm fbody = (ASTTerm) terms.get(1); 
      String fstat = fbody.toKM3(); 

      if (fbody.statement != null) { }
      else 
      { fbody.statement = new InvocationStatement("skip"); } 
      statement = new FinalStatement(fbody.statement);  

      return "    finally " + fstat + "\n"; 
    } 

    if ("switchBlockStatementGroup".equals(tag))
    { // (switchBlockStatementGroup (switchLabel ...)* (blockStatement ...)*

      ASTTerm ctest = (ASTTerm) terms.get(0); 
      if (terms.size() > 1)
      { ASTTerm code = (ASTTerm) terms.get(1); 
        return ctest.toKM3() + " do " + code.toKM3() + "\n";
      }  
      return ctest.toKM3() + " do skip\n";
    } 

    if ("switchLabel".equals(tag))
    { // (switchLabel case expr :)
      // (switchLabel default :)

      if ("case".equals(terms.get(0) + ""))
      { ASTTerm test = (ASTTerm) terms.get(1); 
        // if (terms.size() > 2)
        // { ASTTerm code = (ASTTerm) terms.get(2); 
        //   return "  case " + test.toKM3() + " do " + code.toKM3() + "\n"; 
        // } 
        return "      case " + test.toKM3(); 
        // + " do skip\n"; 
      }
      else if ("default".equals(terms.get(0) + ""))
      { // if (terms.size() > 1)
        // { ASTTerm code = (ASTTerm) terms.get(1); 
        //   return "  endswitch " + code.toKM3() + "\n"; 
        // } 
        return "    endswitch"; // do skip\n"; 
      } 
    }
   
    if ("arrayInitializer".equals(tag))
    { String res = "Sequence{";
      Vector elems = new Vector(); 
 
      for (int i = 0; i < terms.size(); i++) 
      { ASTTerm tt = (ASTTerm) terms.get(i); 
        res = res + tt.toKM3();
        if (tt.expression != null) 
        { elems.add(tt.expression); } 
      } 
      res = res + "}";
      ASTTerm.setType(this,"Sequence");

      expression = new SetExpression(elems, true); 
  
      return res; 
    } 
      

    if ("localVariableDeclaration".equals(tag) && terms.size() > 1)
    { // (localVariableDeclaration type (variableDeclarators ...))
      // (localVariableDeclaration modifiers type (variableDeclarators ...))

      String res = ""; 
      ASTTerm typeTerm; 
      ASTCompositeTerm varTerm;
 
      if (terms.size() >= 3) 
      { typeTerm = (ASTTerm) terms.get(terms.size()-2); 
        varTerm = (ASTCompositeTerm) terms.get(terms.size()-1); 
      } // ignore modifiers 
      else 
      { typeTerm = (ASTTerm) terms.get(0); 
        varTerm = (ASTCompositeTerm) terms.get(1); 
      } 

      String km3type = typeTerm.toKM3();
      Type actualType = (Type) typeTerm.modelElement; 

      Vector vardeclarators = varTerm.terms; 
      for (int i = 0; i < vardeclarators.size(); i++) 
      { ASTTerm vTerm = (ASTTerm) vardeclarators.get(i);
        if (vTerm instanceof ASTCompositeTerm)  
        { ASTCompositeTerm tv = (ASTCompositeTerm) vTerm; 
          String km3var = tv.toKM3Var(); 
          String km3init = tv.toKM3VarInit(); 
 
          if (res.equals("")) { } 
          else 
          { res = res + " ; "; } 
 
          res = res + "var " + km3var + " : " + km3type; 
 
          if (km3init != null) 
          { res = res + " ; " + km3var + " := " + km3init; }  
 
          ASTTerm.setType(km3var,km3type);
 
          BasicExpression varbe =
            BasicExpression.newVariableBasicExpression(km3var, km3type, 
                         ASTTerm.enumtypes, ASTTerm.entities);  
          statement = CreationStatement.newCreationStatement(km3var, km3type, ASTTerm.enumtypes, ASTTerm.entities);
          if (actualType != null) 
          { varbe.setType(actualType); 
            ((CreationStatement) statement).setType(actualType); 
          } 
           
          System.out.println(">> Type of " + varbe + " is " + varbe.getType());
          System.out.println(); 

          ASTTerm vInit = tv.varInit(); 
          if (vInit != null && vInit.expression != null)
          { AssignStatement initStat = new AssignStatement(varbe,vInit.expression); 
            SequenceStatement sstatement = new SequenceStatement(); 
            sstatement.addStatement(statement); 
            sstatement.addStatement(initStat);
          
            if (tv.statement != null) // post side-effect 
            { sstatement.addStatement(tv.statement); } 
           
            statement = sstatement; 
          }

          System.out.println(">> Declaration statements: " + statement);
          System.out.println(); 
        } 
      }  
      return res; 
    }   

    if ("fieldDeclaration".equals(tag) && terms.size() > 1)
    { ASTTerm typeTerm = (ASTTerm) terms.get(0); 
      ASTCompositeTerm varTerm = (ASTCompositeTerm) terms.get(1); 
      String km3type = typeTerm.toKM3();
      Type actualType = (Type) typeTerm.modelElement; 
 
      String km3var = varTerm.toKM3Var(); 
      String km3init = varTerm.toKM3VarInit(); 
      String res = "  attribute " + km3var + " : " + km3type; 
      // if (km3init != null) 
      // { res = res + " := " + km3init; }  
      ASTTerm.setType(km3var,km3type);
      Attribute att = Attribute.newAttribute(km3var, km3type, ASTTerm.enumtypes, ASTTerm.entities); 
      if (actualType != null) 
      { att.setType(actualType); } 
      modelElement = att;  
      // and set its initialisation. 

      System.out.println(">> Type of " + km3var + " is " + km3type); 
      System.out.println(">> Attribute = " + att); 
      System.out.println(); 
      return res + ";\n"; 
    }   

    if ("typeType".equals(tag))
    { if (terms.size() >= 3 && 
          "[".equals(terms.get(1) + "") &&
          "]".equals(terms.get(2) + ""))
      { ASTTerm typeTerm = (ASTTerm) terms.get(0); 
        String tt = typeTerm.toKM3();
        Type elementType = (Type) typeTerm.modelElement; 

        for (int i = 1; i+1 < terms.size(); i = i + 2) 
        { if ((terms.get(i) + "").equals("[") &&
              (terms.get(i+1) + "").equals("]"))
          { tt = "Sequence(" + tt + ")"; 

            modelElement = new Type("Sequence", null);
            if (elementType != null) 
            { ((Type) modelElement).setElementType(elementType); }
            elementType = (Type) modelElement;   
          } 
        } 
        return tt;  
      } 
      else if (terms.size() == 1) 
      { ASTTerm typeTerm = (ASTTerm) terms.get(0); 
        String tt = typeTerm.toKM3();
        modelElement = typeTerm.modelElement;
        expression = typeTerm.expression; 
        System.out.println(modelElement); 
        System.out.println(expression); 
        System.out.println(); 
        return tt; 
      } 
    } 

    if ("typeTypeOrVoid".equals(tag))
    { if (terms.size() == 1) 
      { ASTTerm typeTerm = (ASTTerm) terms.get(0); 
        String tt = typeTerm.toKM3();
        modelElement = typeTerm.modelElement; 
        return tt; 
      } 
    } 

    if ("typeParameters".equals(tag))
    { if (terms.size() == 3) 
      { ASTTerm typeTerm = (ASTTerm) terms.get(1); 
        String tt = typeTerm.toKM3();
        modelElement = typeTerm.modelElement; 
        return tt; 
      } 
    } 

    if ("classBodyDeclaration".equals(tag) ||
        "interfaceBodyDeclaration".equals(tag))
    { if (terms.size() == 1) 
      { ASTTerm memberTerm = (ASTTerm) terms.get(0); 
        String tt = memberTerm.toKM3();
        modelElement = memberTerm.modelElement; 
        modelElements = memberTerm.modelElements; 
        return tt; 
      } 
      else if (terms.size() == 2 && 
               "static".equals(terms.get(0) + "") && 
               ((ASTTerm) terms.get(1)).hasTag("block"))
      { // static block
        ASTTerm stat = (ASTTerm) terms.get(1); 
        String blockcode = stat.toKM3();
        String initId = 
          Identifier.nextIdentifier("initialiseClass");  
        BehaviouralFeature bf = 
          new BehaviouralFeature(initId); 
        bf.setPre(new BasicExpression(true)); 
        bf.setPost(new BasicExpression(true)); 
        bf.setParameters(new Vector()); 
        bf.setStatic(true); 
        if (stat.statement != null) 
        { bf.setActivity(stat.statement); } 
        modelElement = bf; 
        modelElements = stat.modelElements; 
        return "  static operation " + initId + "()\n" + 
               "  pre: true post: true\n" + 
               "  activity: " + blockcode + ";\n\n"; 
      } 
      else 
      { String res = ""; 
        boolean isStatic = false; 

        for (int i = 0; i < terms.size(); i++) 
        { ASTTerm t = (ASTTerm) terms.get(i); 
          String tt = t.toKM3();
          if (t.modelElement != null) 
          { modelElement = t.modelElement; } 
          if (t.modelElements != null) 
          { modelElements = t.modelElements; }
          if ("static".equals(t.literalForm()))
          { isStatic = true; }  
          res = res + tt; 
        }  

        if (isStatic && modelElement != null)
        { if (modelElement instanceof BehaviouralFeature)
          { ((BehaviouralFeature) modelElement).setStatic(true); }
          else if (modelElement instanceof Attribute)
          { ((Attribute) modelElement).setStatic(true); }
        } 

        return res; 
      } // modifiers before the member
    } 

    if ("memberDeclaration".equals(tag) || "interfaceMemberDeclaration".equals(tag))
    { if (terms.size() == 1) 
      { ASTTerm memberTerm = (ASTTerm) terms.get(0); 
        String tt = memberTerm.toKM3();
        modelElement = memberTerm.modelElement;
        modelElements = memberTerm.modelElements;  
        return tt; 
      } 
      else 
      { String res = ""; 
        for (int i = 0; i < terms.size(); i++) 
        { ASTTerm t = (ASTTerm) terms.get(i); 
          String tt = t.toKM3();
          if (t.modelElement != null) 
          { modelElement = t.modelElement; } 
          if (t.modelElements != null) 
          { modelElements = t.modelElements; } 
          res = res + tt; 
        }  
        return res; 
      } // modifiers before the member
    } 

    if ("classOrInterfaceType".equals(tag))
    { if (terms.size() > 1)  // A parameterised type 
      { ASTTerm baseType = (ASTTerm) terms.get(0); 
        ASTTerm typepars = (ASTTerm) terms.get(1);
        String pars = typepars.toKM3();
        
        Vector targs = 
          ((ASTCompositeTerm) typepars).getTypeParameterTypes(); 

        System.out.println(); 

        System.out.println(">> Parameterised type with parameters " + targs); 
        System.out.println(); 
 
        String btype = baseType.toKM3();
        modelElement = baseType.modelElement; 
        expression = baseType.expression; // Not valid
        if (modelElement != null)
        { if (targs.size() > 1) // Maps, Functions
          { Type ktype = (Type) targs.get(0); 
            Type etype = (Type) targs.get(1); 
            ((Type) modelElement).setKeyType(ktype); 
            ((Type) modelElement).setElementType(etype);
          }  
          else if (targs.size() > 0) 
          { Type etype = (Type) targs.get(0); 
            ((Type) modelElement).setElementType(etype);
          }  
          expression = new BasicExpression((Type) modelElement); 
          System.out.println(modelElement);
          System.out.println(expression); 
          System.out.println(); 
        }  
        return btype + "(" + pars + ")"; 
      }
      else 
      { ASTTerm t0 = (ASTTerm) terms.get(0);
        String res = t0.toKM3();
        modelElement = t0.modelElement;
        expression = t0.expression;  
        return res;  
      } 
    } 
 
    if ("typeArguments".equals(tag) && terms.size() > 1)
    { // < arg >   or   < arg1 , arg2 >
      ASTTerm typepar0 = (ASTTerm) terms.get(1); 
      String res = typepar0.toKM3();
      modelElement = typepar0.modelElement; 
 
      if (terms.size() > 3)
      { ASTTerm typepar1 = (ASTTerm) terms.get(3); 
        res = res + ", " + typepar1.toKM3(); 
      } 
      return res; 
    } 

    if ("typeArgument".equals(tag) && terms.size() > 0)
    { // a (typeType _1)
      ASTTerm typepar0 = (ASTTerm) terms.get(0); 
      String res = typepar0.toKM3();
      modelElement = typepar0.modelElement; 
 
      return res; 
    } 

    if ("enhancedForControl".equals(tag))
    { ASTTerm typeTerm = (ASTTerm) terms.get(0); 
      ASTTerm varTerm = (ASTTerm) terms.get(1); 
      ASTTerm rangeTerm = (ASTTerm) terms.get(3); 
      // String km3type = typeTerm.toKM3(); 
      String km3var = varTerm.toKM3(); 
      String km3range = rangeTerm.toKM3(); 
      String res = km3var + " : " + km3range; 
      if (varTerm.expression != null && 
          rangeTerm.expression != null) 
      { expression = 
          new BinaryExpression(":", varTerm.expression, 
                               rangeTerm.expression); 
      } 

      // if (km3var != null) 
      // { res = res + " := " + km3init; }  
      return res; 
    }   

    if ("blockStatement".equals(tag))
    { if (terms.size() > 0) 
      { ASTTerm tt = (ASTTerm) terms.get(0); 
        String res = tt.toKM3(); 
        statement = tt.statement; 
        return res; 
      } 
    } 

    if ("block".equals(tag))
    { System.out.println(">> Statement block with " + terms.size() + " terms " + terms); 

      if (terms.size() <= 2)
      { statement = new InvocationStatement("skip"); 
        return "skip"; 
      } 
      else if (terms.size() == 3) // Single statement
      { ASTTerm t = (ASTTerm) terms.get(1);
        String stat = t.toKM3(); 

        if (t.statement != null)  
        { statement = new SequenceStatement(); 
          ((SequenceStatement) statement).addStatement(t.statement); 
        }

        return "( " + stat + " )"; 
      } 
      else // (terms.size() >= 2) // Series of statements
      { String res = "";
        int count = 0;  

        statement = new SequenceStatement(); 

        ASTTerm t0 = (ASTTerm) terms.get(0); 

        String prev = t0.toKM3();

        if (t0.statement != null) 
        { ((SequenceStatement) statement).addStatement(t0.statement); }

          
        for (int i = 1; i < terms.size(); i++) 
        { ASTTerm tt = (ASTTerm) terms.get(i); 
          String next = tt.toKM3(); 
          
          if (tt.statement != null) 
          { ((SequenceStatement) statement).addStatement(tt.statement); } 

          if (prev.length() > 0) 
          { res = res + " " + prev; 
            count++; 
          }  
          if (prev.length() > 0 && next.length() > 0)
          { res = res + " ;\n  "; }
          prev = next; 
        }
 
        if (prev.length() > 0) 
        { res = res + " " + prev; 
          count++; 
        }  
          
        if (count > 1) 
        { res = " ( " + res + " )"; }

        System.out.println(">>> Statement block: " + statement); 
        System.out.println(); 
 
        return res + " ";  
      } 
    } 

    if ("genericMethodDeclaration".equals(tag))
    { // (typeParameters < (typeParameter T) ... > )
      // (methodDeclaration ...)
      ASTTerm typePars = (ASTTerm) terms.get(0); 
      ASTTerm mDec = (ASTTerm) terms.get(1); 

      String gtype = typePars.toKM3(); 
      System.out.println(">> Generic type= " + gtype); 

      Entity parEnt = new Entity(gtype); 
      ASTTerm.entities.add(parEnt); 
      Type parEntType = new Type(parEnt); 

      String met = mDec.toKM3(); 
      modelElement = mDec.modelElement; 

      String res = met; 

      if (modelElement != null && 
          modelElement instanceof BehaviouralFeature)
      { BehaviouralFeature bf = (BehaviouralFeature) modelElement;
        bf.addTypeParameter(parEntType);
        String bfname = bf.getName();  
        res = met.replace(bfname, bfname + "<" + gtype + ">"); 
      }  

      ASTTerm.entities.remove(parEnt); 

      return res; 
    } // replace operation name by name<gtype>

    if ("methodDeclaration".equals(tag) || 
        "interfaceMethodDeclaration".equals(tag))
    { ASTTerm mtype = (ASTTerm) terms.get(0); 
      ASTTerm mname = (ASTTerm) terms.get(1);
      ASTTerm mparams = (ASTTerm) terms.get(2); 
      
      BehaviouralFeature bf = new BehaviouralFeature(mname.literalForm()); 

      String restype = mtype.toKM3(); 
      String res = "  operation " + mname + mparams.toKM3() + " : " + restype  + "\n" + 
              "  pre: true\n" + "  post: true"; 

      if (mtype.modelElement instanceof Type)
      { bf.setType((Type) mtype.modelElement); } 
      else if (restype != null) 
      { Type resT = Type.getTypeFor(restype, ASTTerm.enumtypes, ASTTerm.entities); 
        if (resT != null) 
        { bf.setType(resT); } 
      } 

      if (mparams.modelElements != null) 
      { bf.setParameters(mparams.modelElements); } 

      bf.setPrecondition(new BasicExpression(true)); 
      bf.setPostcondition(new BasicExpression(true)); 

      modelElement = bf; 

      if (terms.size() > 3)
      { ASTTerm mbody = (ASTTerm) terms.get(3);

        if (";".equals(mbody.literalForm()))
        { return res + ";\n"; }  
        res = res + "\n  activity:\n"; 

        for (int i = 3; i < terms.size(); i++) 
        { ASTTerm tt = (ASTTerm) terms.get(i);
          if ("throws".equals(tt.literalForm()))
          { i++; } 
          else  
          { res = res + "    " + tt.toKM3() + "\n"; 
            if (tt.statement != null)
            { bf.setActivity(tt.statement);
               
              System.out.println(">>> Operation " + bf + " has activity " + tt.statement); 
              System.out.println(); 
            } 
          }
        }
      }  
      return res + "  ;\n\n"; 
    }

    if ("methodBody".equals(tag))
    { ASTTerm code = (ASTTerm) terms.get(0); 
      String res = code.toKM3();
      statement = code.statement; 
    }

    if ("classBody".equals(tag) || "interfaceBody".equals(tag))
    { modelElements = new Vector();
      String res = ""; 
  
      for (int i = 0; i < terms.size(); i++) 
      { ASTTerm t = (ASTTerm) terms.get(i); 
        res = res + t.toKM3(); 
        if (t.modelElement != null) 
        { modelElements.add(t.modelElement); }
        else if (t.modelElements != null) 
        { modelElements.addAll(t.modelElements); }  
      } 
      return res; 
    } 
         

    if ("constructorDeclaration".equals(tag))
    { ASTTerm mname = (ASTTerm) terms.get(0);
      ASTTerm mparams = (ASTTerm) terms.get(1); 
      String cname = mname.literalForm(); 
      Entity cent = (Entity)
          ModelElement.lookupByName(cname, 
                                    ASTTerm.entities); 
    
      String res = "\n  static operation new" + cname + mparams.toKM3() + " : " + cname + "\n" + 
              "  pre: true\n" + "  post: true\n"; 
      res = res + 
          "  activity:\n" + 
          "  ( var result : " + cname + " := create" + cname + "() ;\n" +  
          "    result.initialise(" +  getParNameList(mparams) + ") ;\n" + 
          "    return result );\n\n"; 
   
      BehaviouralFeature constr = 
        BehaviouralFeature.newConstructor(cname,
                                 mparams.modelElements); 
      constr.setStatic(true); 
 
      res = res + "  operation initialise" + mparams.toKM3() + " : void\n" + 
              "  pre: true\n" + "  post: true\n"; 

      SequenceStatement initCode = new SequenceStatement(); 
      
      if (terms.size() > 2)
      { res = res + 
          "  activity:\n";  
        for (int i = 2; i < terms.size(); i++) 
        { ASTTerm tt = (ASTTerm) terms.get(i); 
          res = res + "   " + tt.toKM3();

          if (tt.statement != null) 
          { initCode.addStatement(tt.statement); } 

          if (i < terms.size() - 1) 
          { res = res + "\n"; }
          else 
          { res = res + " ;\n"; }  
        }
      }  

      BehaviouralFeature bfInit = new BehaviouralFeature("initialise"); 
      bfInit.setParameters(mparams.modelElements); 
      bfInit.setActivity(initCode); 
      bfInit.setPrecondition(new BasicExpression(true)); 
      bfInit.setPostcondition(new BasicExpression(true)); 
      // modelElement = bfInit; 

      modelElements = new Vector(); 
      modelElements.add(bfInit); 
      modelElements.add(constr); 

      if (cent != null && cent.getSuperclass() != null) 
      { Entity supent = cent.getSuperclass(); 
        BehaviouralFeature overriddenOp = 
           supent.getDefinedOperation("initialise", 
                                      mparams.modelElements); 
        if (overriddenOp != null) 
        { bfInit.addStereotype("override"); } 
      } 

      return res + "\n\n"; 
    }

    if ("formalParameter".equals(tag))
    { int nTerms = terms.size(); 
      ASTTerm mtype = (ASTTerm) terms.get(nTerms - 2); 
      ASTTerm mname = (ASTTerm) terms.get(nTerms - 1);
      
      String typ = mtype.toKM3(); 
      String vv = mname.toKM3(); 
      ASTTerm.setType(vv,typ);

      modelElement = Attribute.newAttribute(vv, typ, ASTTerm.enumtypes, ASTTerm.entities);  
  
      String res = vv + " : " + typ;  
      return res;
    }

    if ("formalParameters".equals(tag))
    { String res = ""; 

      modelElements = new Vector(); 

      for (int i = 0; i < terms.size(); i++) 
      { ASTTerm pterm = (ASTTerm) terms.get(i); 
        res = res + pterm.toKM3(); 
        if (pterm.modelElement != null) 
        { modelElements.add(pterm.modelElement); } 
        else if (pterm.modelElements != null) 
        { modelElements.addAll(pterm.modelElements); } 
      } 

      return res;
    }

    if ("formalParameterList".equals(tag))
    { String res = ""; 

      modelElements = new Vector(); 

      for (int i = 0; i < terms.size(); i++) 
      { ASTTerm pterm = (ASTTerm) terms.get(i); 
        res = res + pterm.toKM3(); 
        if (pterm.modelElement != null) 
        { modelElements.add(pterm.modelElement); } 
      } 

      return res;
    }

    if ("importDeclaration".equals(tag))
    { return "\n"; }  


    if ("enumDeclaration".equals(tag) && 
        terms.size() > 3)
    { ASTTerm ename = (ASTTerm) terms.get(1); 
      ASTTerm literals = (ASTTerm) terms.get(3);
      Vector litvals = ((ASTCompositeTerm) literals).literalValues(); 

      Type etype = new Type(ename.literalForm(), litvals); 
      modelElement = etype; 
      ASTTerm.enumtypes.add(etype); 
 
      return "enumeration " + ename +
             " {\n" + literals.toKM3() + "}\n\n"; 
    }  

    if ("modifier".equals(tag))
    { if (terms.size() > 0)
      { String ltf = ((ASTTerm) terms.get(0)).literalForm(); 
        if ("static".equals(ltf))
        { return "  static "; } 
      } 
      return ""; 
    } 

    if ("enumConstants".equals(tag))
    { String res = ""; 
      for (int i = 0; i < terms.size(); i++) 
      { ASTTerm lit = (ASTTerm) terms.get(i); 
        if (",".equals(lit + "")) { } 
        else
        { res = res + "  literal " + lit.toKM3() +
                ";\n";
        } 
      } 
      return res;  
    }  

    if ("packageDeclaration".equals(tag))
    { ASTTerm pname = (ASTTerm) terms.get(1); 
      String pn = pname.toKM3(); 
      ASTTerm.packageName = pn;
      return ""; 
    } 

    if ("compilationUnit".equals(tag))
    { String res = ""; 
      modelElements = new Vector(); 

      for (int i = 0; i < terms.size(); i++) 
      { ASTTerm t = (ASTTerm) terms.get(i); 
        res = res + t.toKM3(); 
        if (t.modelElement != null) 
        { modelElements.add(t.modelElement); } 
        else if (t.modelElements != null) 
        { modelElements.addAll(t.modelElements); } 
      } 
      return res; 
    } 

    if ("typeDeclaration".equals(tag))
    { String res = ""; 
      modelElements = new Vector(); 

      for (int i = 0; i < terms.size(); i++) 
      { ASTTerm t = (ASTTerm) terms.get(i); 
        
        String lfterm = t.literalForm(); 

        if ("abstract".equals(lfterm) ||
            "public".equals(lfterm) ||
            "private".equals(lfterm) || 
            "protected".equals(lfterm)) 
        { continue; } 

        res = res + t.toKM3(); 
        if (t.modelElement != null) 
        { modelElements.add(t.modelElement); 
          modelElement = t.modelElement; 
        } 
        else if (t.modelElements != null) 
        { modelElements.addAll(t.modelElements); } 
      } 
      return res; 
    } 

        
    
    if ("classDeclaration".equals(tag) && 
        terms.size() > 2)
    { ASTTerm ename = (ASTTerm) terms.get(1); 
      String entName = ename.literalForm(); 
      
      Entity newEnt = (Entity) ModelElement.lookupByName(entName, ASTTerm.entities); 
      if (newEnt == null) 
      { newEnt = new Entity(entName);
        ASTTerm.entities.add(newEnt); 
      } 

      ASTTerm.currentClass = newEnt; 

      int bodyIndex = 2;  

      String typePar = ""; 

      ASTTerm contents = (ASTTerm) terms.get(2);
      if (contents.hasTag("typeParameters"))
      { // Generic class 
        typePar = "<" + contents.toKM3() + ">"; 
        System.out.println(">>> Generic class, parameter: " + typePar);
 
        if (contents.modelElement != null && 
            (contents.modelElement instanceof Type))
        { Entity newT = 
            new Entity(contents.modelElement.getName()); 
          ASTTerm.entities.add(newT); 
          newEnt.addTypeParameter(new Type(newT));
          System.out.println(">>> Type parameter " + newT);  
        } 

        bodyIndex = 3; 
        contents = (ASTTerm) terms.get(3); 
      } 

      if ("extends".equals(contents.literalForm()) && 
          terms.size() > 4)
      { ASTTerm superclass = (ASTTerm) terms.get(bodyIndex+1);
        String sclass = superclass.toKM3();  

        String supclass = sclass; 
        String interfaceList = ""; 

        contents = (ASTTerm) terms.get(bodyIndex+2); 
        
        String km3Contents = contents.toKM3(); 
        if ("implements".equals(km3Contents))
        { ASTTerm typeList = (ASTTerm) terms.get(bodyIndex+3);
          String iList = typeList.toKM3(); 
          interfaceList = typeList.literalForm(); 

          sclass = sclass + ", " + iList; 
          
          // add as interfaces of newEnt
          contents = (ASTTerm) terms.get(bodyIndex+4); 
          km3Contents = contents.toKM3();
        } 

        Vector entPars = newEnt.getTypeParameters(); 
        for (int kk = 0; kk < entPars.size(); kk++) 
        { Type tp = (Type) entPars.get(kk); 
          if (tp.isEntity())
          { ASTTerm.entities.remove(tp.entity); } 
        } 

        if (contents.modelElements != null) 
        { newEnt.addModelElements(contents.modelElements);
          System.out.println(">>> Model elements of " + newEnt + " are: " + contents.modelElements);
        } 

        Entity sup = 
          (Entity) ModelElement.lookupByName(
                          supclass, ASTTerm.entities); 
        if (sup == null) 
        { sup = new Entity(supclass); 
          ASTTerm.entities.add(sup); 
        } 
        newEnt.setSuperclass(sup);
        newEnt.addInterfaces(interfaceList,  ASTTerm.entities);  
        modelElement = newEnt; 
        String stereotypeList = newEnt.stereotypesKM3(); 

        if (newEnt.getInterfaces() == null || 
            newEnt.getInterfaces().size() == 0) 
        { return "class " + ename + typePar + " extends " + supclass + 
             " {\n" + stereotypeList + km3Contents + "}\n\n"; 
        } 
 
        return "class " + ename + typePar + 
             " extends " + sclass +
             " {\n" + stereotypeList + km3Contents + "}\n\n"; 
      } 
      else if ("implements".equals(contents.literalForm()) && 
          terms.size() > 4)
      { ASTTerm superclass = (ASTTerm) terms.get(bodyIndex + 1);
        String sclass = superclass.toKM3();
        // But may be several or none 
  
        contents = (ASTTerm) terms.get(bodyIndex+2); 
        String km3Contents = contents.toKM3(); 

        if (contents.modelElements != null) 
        { System.out.println(">>> Model elements of " + 
            newEnt + " are: " + contents.modelElements);
          newEnt.addModelElements(contents.modelElements); 
        } 
        
        // Entity sup = new Entity(sclass); 
        newEnt.addInterfaces(superclass.literalForm(), ASTTerm.entities); 
        // newEnt.setSuperclass(sup); 
        modelElement = newEnt; 

        // ASTTerm.entities.add(newEnt); 

        String stereotypeList = newEnt.stereotypesKM3(); 
 
        if (newEnt.getInterfaces() == null || 
            newEnt.getInterfaces().size() == 0) 
        { return "class " + ename + typePar +
             " {\n" + stereotypeList + km3Contents + "}\n\n"; 
        } 

        return "class " + ename + typePar + " implements " + sclass +
             " {\n" + stereotypeList + km3Contents + "}\n\n"; 
      } 
      else 
      { String km3Contents = contents.toKM3(); 

        if (contents.modelElements != null) 
        { newEnt.addModelElements(contents.modelElements); } 
        modelElement = newEnt; 

        // ASTTerm.entities.add(newEnt); 

        return "class " + ename + typePar + 
             " {\n" + km3Contents + "}\n\n"; 
      } 

    }  

    // Can also be generic: 
    if ("interfaceDeclaration".equals(tag) && 
        terms.size() > 2)
    { ASTTerm ename = (ASTTerm) terms.get(1); 

      String entName = ename.literalForm(); 
      Entity newEnt = 
        (Entity) ModelElement.lookupByName(
                        entName, ASTTerm.entities); 
      if (newEnt == null) 
      { newEnt = new Entity(entName);
        ASTTerm.entities.add(newEnt); 
      }  

      ASTTerm.currentClass = newEnt; 

      ASTTerm contents = (ASTTerm) terms.get(2);

      if ("extends".equals(contents.literalForm()) && 
          terms.size() > 4)
      { ASTTerm superclass = (ASTTerm) terms.get(3);
        String sclass = superclass.toKM3();  
        contents = (ASTTerm) terms.get(4); 
        String km3Contents = contents.toKM3(); 

        if (contents.modelElements != null) 
        { newEnt.addModelElements(contents.modelElements); } 

        Entity sup = 
          (Entity) ModelElement.lookupByName(
                         sclass, ASTTerm.entities); 
        if (sup == null) 
        { sup = new Entity(sclass); 
          ASTTerm.entities.add(sup); 
        } 
        newEnt.setSuperclass(sup);

        newEnt.addStereotype("interface");  
        modelElement = newEnt; 
 
        return "class " + ename + " extends " + sclass +
             " { stereotype interface;\n" + km3Contents + "}\n\n"; 
      } 
      else 
      { String km3Contents = contents.toKM3(); 

        if (contents.modelElements != null) 
        { newEnt.addModelElements(contents.modelElements); } 
        newEnt.addStereotype("interface");  
        modelElement = newEnt; 

        ASTTerm.entities.add(newEnt); 

        return "class " + ename +
             " { stereotype interface;\n" + contents.toKM3() + "}\n\n"; 
      } 

    }  


    if (terms.size() == 1) // Identifier or literal
    { ASTTerm t = (ASTTerm) terms.get(0); 
      return t.toKM3();     
    } 

    String res = "";  
    for (int i = 0; i < terms.size(); i++) 
    { ASTTerm tt = (ASTTerm) terms.get(i); 
      res = res + tt.toKM3() + " "; 
    } 
    return res; 
  }

  public boolean updatesObject(ASTTerm t)
  { if ("methodCall".equals(tag))
    { 
      String called = terms.get(0) + "";
      if (("add".equals(called) && t != null && t.isCollection()) || 
          "addAll".equals(called) || 
          "addFirst".equals(called) || 
          "addLast".equals(called) ||
          "addElement".equals(called) ||
          "append".equals(called) ||
          "offer".equals(called) ||  
          "copyInto".equals(called) ||  
          "put".equals(called) || "putAll".equals(called) || 
          "removeAll".equals(called) || 
          "remove".equals(called) ||
          "removeFirst".equals(called) || 
          "removeLast".equals(called) ||
          "insert".equals(called) || 
          "insertElementAt".equals(called) || 
          "removeAllElements".equals(called) ||  
          "set".equals(called) ||
          "setElementAt".equals(called) || 
          "setSize".equals(called) || 
          "setLength".equals(called) || 
          "removeRange".equals(called) || 
          "clear".equals(called) || "push".equals(called) || 
          "removeElement".equals(called) || 
          "removeElementAt".equals(called) ||
          "delete".equals(called) || 
          "deleteCharAt".equals(called) ||
          "setCharAt".equals(called) ||
          ("replace".equals(called) && t != null && 
           t.isSequence()) ||
          "reverse".equals(called) ||  
          "pop".equals(called) || "take".equals(called) ||
          "poll".equals(called) ||  
          "retainAll".equals(called))
      { System.out.println(">>> methodCall " + called + " updates the object it is applied to."); 
        return true; 
      } 
    }
    else if ("expression".equals(tag))
    { if (terms.size() > 2 && ".".equals(terms.get(1) + ""))
      { ASTTerm call = (ASTTerm) terms.get(2); 
        return call.updatesObject((ASTTerm) terms.get(0)); 
      } 
      if (terms.size() == 4 && 
          "(".equals(terms.get(0) + "") &&
          ")".equals(terms.get(2) + ""))
      { // casting 
        ASTTerm call = (ASTTerm) terms.get(3); 
        return call.updatesObject(null); 
      } 
    } 
    return false; 
  } 

  public boolean hasSideEffect()
  { if ("methodCall".equals(tag))
    { 
      String called = terms.get(0) + "";
      if ("replaceAll".equals(called))
      { System.out.println(">>> methodCall " + called + " has side-effect."); 
        return true; 
      }
      return false;  
    } 

    if ("primary".equals(tag) || "parExpression".equals(tag)) 
    { for (int i = 0; i < terms.size(); i++) 
      { ASTTerm tt = (ASTTerm) terms.get(i); 
        if (tt.hasSideEffect()) 
        { return true; } 
      } 
      return false; 
    } 

    if ("expression".equals(tag))
    { if (terms.size() == 1) // Identifier or literal
      { 
        ASTTerm t = (ASTTerm) terms.get(0); 
        return t.hasSideEffect();          
      } 

      if (terms.size() == 2) // UnaryExpression
      { ASTTerm op = (ASTTerm) terms.get(0); 
        ASTTerm arg = (ASTTerm) terms.get(1);

        String op1 = op.toKM3(); 
        String arg1 = arg.toKM3(); 

        if ("++".equals(arg1) || "++".equals(op1))
        { return true; }
 
        if ("--".equals(arg1) || "--".equals(op1))
        { return true; }
      }  
      else if (terms.size() == 3) // BinaryExpression
      { ASTTerm op = (ASTTerm) terms.get(1); 
        ASTTerm arg1 = (ASTTerm) terms.get(0);
        ASTTerm arg2 = (ASTTerm) terms.get(2); 

        if (".".equals(terms.get(1) + ""))
        { if ("Collections".equals(arg1.literalForm()) && 
              arg2.hasSideEffect())
          { return true; }  
        } 
      
        return arg1.hasSideEffect() || arg2.hasSideEffect(); 
      } 
      else if (terms.size() == 4 && 
          "(".equals(terms.get(0) + "") &&
          ")".equals(terms.get(2) + ""))
      { // casting 
        ASTTerm call = (ASTTerm) terms.get(3); 
        return call.hasSideEffect(); 
      } 
      else if (terms.size() == 4 && 
          "[".equals(terms.get(1) + "") && 
          "]".equals(terms.get(3) + "")) // array access
      { ASTTerm arr = (ASTTerm) terms.get(0); 
        ASTTerm ind = (ASTTerm) terms.get(2);

        if (arr.hasSideEffect()) 
        { return true; }  
        return ind.hasSideEffect(); 
      }
      else if (terms.size() == 5 && "?".equals(terms.get(1) + ""))
      { // ConditionalExpression
        ASTTerm cond = (ASTTerm) terms.get(0); 
        ASTTerm ifoption = (ASTTerm) terms.get(2);
        ASTTerm elseoption = (ASTTerm) terms.get(4);
        boolean condx = cond.hasSideEffect(); 
        boolean ifx = ifoption.hasSideEffect(); 
        boolean elsex = elseoption.hasSideEffect();
          
        return (condx || ifx || elsex);  
      } 
    } 
    return false; 
  } 

  public String preSideEffect()
  { if ("primary".equals(tag) || "parExpression".equals(tag)) 
    { for (int i = 0; i < terms.size(); i++) 
      { ASTTerm tt = (ASTTerm) terms.get(i); 
        String se = tt.preSideEffect(); 
        if (se != null) 
        { statement = tt.statement; 
          return se; 
        } 
      } 
    } 

    if ("expression".equals(tag))
    { if (terms.size() == 1) // Identifier or literal
      { 
        ASTTerm t = (ASTTerm) terms.get(0); 
        String res = t.preSideEffect(); 
        statement = t.statement; 
        return res;          
      } 
   
      if (terms.size() == 2) // UnaryExpression
      { ASTTerm op = (ASTTerm) terms.get(0); 
        ASTTerm arg = (ASTTerm) terms.get(1);

        String op1 = op.toKM3(); 
        String arg1 = arg.toKM3(); 

        if ("++".equals(op.literalForm()))
        { if (arg.expression != null) 
          { expression = new BinaryExpression("+", 
                              arg.expression, unitExpression); 
            statement = 
              new AssignStatement(arg.expression, expression);
          }  
          
          return arg1 + " := " + arg1 + " + 1"; 
        }
 
        if ("--".equals(op.literalForm()))
        { if (arg.expression != null) 
          { expression = new BinaryExpression("-", 
                              arg.expression, unitExpression); 
            statement = 
              new AssignStatement(arg.expression, expression);
          }  
          
          return arg1 + " := " + arg1 + " - 1"; 
        }
      }  
      else if (terms.size() == 3) // BinaryExpression
      { ASTTerm op = (ASTTerm) terms.get(1); 
        ASTTerm arg1 = (ASTTerm) terms.get(0);
        ASTTerm arg2 = (ASTTerm) terms.get(2); 

        String se1 = arg1.preSideEffect(); 
        Statement stat1 = arg1.statement; 
        String se2 = arg2.preSideEffect();
        Statement stat2 = arg2.statement; 
        
        if (se1 == null && se2 == null) 
        { return null; }  
        
        if (se1 != null && se2 != null) 
        { if (stat1 != null && stat2 != null) 
          { SequenceStatement stat = new SequenceStatement(); 
            stat.addStatement(stat1); 
            stat.addStatement(stat2); 
            statement = stat; 
          }
          return se1 + " ; " + se2; 
        } 

        if (se1 != null) 
        { statement = stat1; 
          return se1; 
        } 

        statement = stat2; 
        return se2;  
      } 
      else if (terms.size() == 4 && 
          "(".equals(terms.get(0) + "") &&
          ")".equals(terms.get(2) + ""))
      { // casting 
        ASTTerm call = (ASTTerm) terms.get(3);
        String res = call.preSideEffect();
        statement = call.statement;
        return res;  
      } 
      else if (terms.size() == 4 && 
          "[".equals(terms.get(1) + "") && 
          "]".equals(terms.get(3) + "")) // array access
      { ASTTerm arr = (ASTTerm) terms.get(0); 
        ASTTerm ind = (ASTTerm) terms.get(2);

        String se1 = arr.preSideEffect();
        Statement stat1 = arr.statement;  
        String se2 = ind.preSideEffect(); 
        Statement stat2 = ind.statement;  
        
        if (se1 == null && se2 == null) 
        { return null; }  
        
        if (se1 != null && se2 != null) 
        { if (stat1 != null && stat2 != null) 
          { SequenceStatement stat = new SequenceStatement(); 
            stat.addStatement(stat1); 
            stat.addStatement(stat2); 
            statement = stat; 
          }
          return se1 + " ; " + se2; 
        }
 
        if (se1 != null) 
        { statement = arr.statement; 
          return se1; 
        }
  
        statement = ind.statement; 
        return se2;  
      }
      else if (terms.size() == 5 && "?".equals(terms.get(1) + ""))
      { // ConditionalExpression
        ASTTerm cond = (ASTTerm) terms.get(0); 
        ASTTerm ifoption = (ASTTerm) terms.get(2);
        ASTTerm elseoption = (ASTTerm) terms.get(4);
        String condx = cond.preSideEffect(); 
        String ifx = ifoption.preSideEffect(); 
        String elsex = elseoption.preSideEffect();
          
        if (condx == null && ifx == null && elsex == null) 
        { return null; }  
        
        String res = ""; 
        SequenceStatement stat = new SequenceStatement(); 

        if (condx != null) 
        { res = res + condx;
          if (cond.statement != null) 
          { stat.addStatement(cond.statement); }  
        } 

        if (ifx != null)
        { if (res.length() > 0)
          { res = res + " ; " + ifx; } 
          else 
          { res = ifx; } 

          if (ifoption.statement != null) 
          { stat.addStatement(ifoption.statement); }  
        } 

        if (elsex != null) 
        { if (res.length() > 0)
          { res = res + " ; " + elsex; } 
          else 
          { res = elsex; } 

          if (elseoption.statement != null) 
          { stat.addStatement(elseoption.statement); }  
        }  

        if (stat.getStatements().size() == 1) 
        { statement = (Statement) stat.getStatement(0); } 
        else 
        { statement = stat; } 

        return res; 
      } 


    } 
    return null; 
  } 

  public String postSideEffect()
  { if ("methodCall".equals(tag))
    { 
      String called = terms.get(0) + "";
      ASTTerm callargs = (ASTTerm) terms.get(2);
      Vector cargs = getCallArguments(callargs); 

      if ("replaceAll".equals(called))
      { System.out.println(">>> methodCall " + called + " has side-effect."); 

        ASTTerm callarg1 = (ASTTerm) cargs.get(0); 
        String callp1 = callarg1.toKM3(); 

        ASTTerm.setType(this,
                          ASTTerm.getType(callarg1)); 
          
        if (cargs.size() > 2) 
        { ASTTerm obj = (ASTTerm) cargs.get(1); 
          ASTTerm rep = (ASTTerm) cargs.get(2);
          String objx = obj.toKM3(); 
          String repx = rep.toKM3();

          if (callarg1.expression != null && 
                obj.expression != null &&  
                rep.expression != null) 
          { Expression xexp = 
                BasicExpression.newVariableBasicExpression("x_1"); 
            Expression eq = 
                new BinaryExpression("=", xexp, obj.expression); 
            Expression conde = 
                new ConditionalExpression(eq,rep.expression,xexp);
            Expression domexpr = 
                new BinaryExpression(":", xexp, callarg1.expression);  
            expression = 
                new BinaryExpression("|C", domexpr, conde); 
            statement = 
                new AssignStatement(callarg1.expression, expression); 
          } 
 
          return callp1 + " := " + callp1 + "->collect(x_1 | if x_1 = " + objx + " then " + repx + " else x_1 endif )"; 
        }
        return callp1;  
      }
      return null; 
    } 
    
    if ("primary".equals(tag) || "parExpression".equals(tag)) 
    { for (int i = 0; i < terms.size(); i++) 
      { ASTTerm tt = (ASTTerm) terms.get(i); 
        String se = tt.postSideEffect(); 
        if (se != null) 
        { expression = tt.expression; 
          statement = tt.statement; 
          return se; 
        } 
      } 
    } 

    if ("expression".equals(tag))
    { if (terms.size() == 1) // Identifier or literal
      { 
        ASTTerm t = (ASTTerm) terms.get(0); 
        String res = t.postSideEffect();         
        expression = t.expression; 
        statement = t.statement; 
        return res; 
      } 
   
      if (terms.size() == 2) // UnaryExpression
      { ASTTerm op = (ASTTerm) terms.get(0); 
        ASTTerm arg = (ASTTerm) terms.get(1);

        String op1 = op.queryForm(); 
        
        if ("++".equals(arg.literalForm()))
        { if (op.expression != null) 
          { expression = new BinaryExpression("+", 
                              op.expression, unitExpression); 
            statement = 
              new AssignStatement(op.expression, expression);
          }  
          return op1 + " := " + op1 + " + 1"; 
        }
 
        if ("--".equals(arg.literalForm()))
        { if (op.expression != null) 
          { expression = new BinaryExpression("-", 
                              op.expression, unitExpression); 
            statement = 
              new AssignStatement(op.expression, expression);
          }  
          return op1 + " := " + op1 + " - 1"; 
        }
      }  
      else if (terms.size() == 3) // BinaryExpression
      { ASTTerm op = (ASTTerm) terms.get(1); 
        ASTTerm arg1 = (ASTTerm) terms.get(0);
        ASTTerm arg2 = (ASTTerm) terms.get(2); 

        if (".".equals(terms.get(1) + ""))
        { if ("Collections".equals(arg1.literalForm()) && 
              arg2.hasSideEffect())
          { String res = arg2.postSideEffect(); 
            statement = arg2.statement; 
            return res; 
          }  
        } 
      

        String se1 = arg1.postSideEffect(); 
        String se2 = arg2.postSideEffect();
        
        if (se1 == null && se2 == null) 
        { return null; }  
        
        if (se1 != null && se2 != null) 
        { if (arg1.statement != null && arg2.statement != null) 
          { statement = new SequenceStatement(); 
            ((SequenceStatement) statement).addStatement(arg1.statement); 
            ((SequenceStatement) statement).addStatement(arg2.statement);
          }
          return se1 + " ; " + se2; 
        } 

        if (se1 != null) 
        { statement = arg1.statement; 
          return se1; 
        }

        statement = arg2.statement;  
        return se2;  
      } 
      else if (terms.size() == 4 && 
          "(".equals(terms.get(0) + "") &&
          ")".equals(terms.get(2) + ""))
      { // casting 
        ASTTerm call = (ASTTerm) terms.get(3);
        String res = call.postSideEffect();
        statement = call.statement; 
        return res;  
      } 
      else if (terms.size() == 4 && 
          "[".equals(terms.get(1) + "") && 
          "]".equals(terms.get(3) + "")) // array access
      { ASTTerm arr = (ASTTerm) terms.get(0); 
        ASTTerm ind = (ASTTerm) terms.get(2);

        String se1 = arr.postSideEffect(); 
        String se2 = ind.postSideEffect(); 
        
        if (se1 == null && se2 == null) 
        { return null; }  
        
        if (se1 != null && se2 != null) 
        { if (arr.statement != null && ind.statement != null) 
          { statement = new SequenceStatement(); 
            ((SequenceStatement) statement).addStatement(arr.statement); 
            ((SequenceStatement) statement).addStatement(ind.statement);
          } 

          return se1 + " ; " + se2; 
        }
 
        if (se1 != null) 
        { statement = arr.statement; 
          return se1; 
        } 

        statement = ind.statement; 
        return se2;  
      }
      else if (terms.size() == 5 && "?".equals(terms.get(1) + ""))
      { // ConditionalExpression
        ASTTerm cond = (ASTTerm) terms.get(0); 
        ASTTerm ifoption = (ASTTerm) terms.get(2);
        ASTTerm elseoption = (ASTTerm) terms.get(4);
        String condx = cond.postSideEffect(); 
        String ifx = ifoption.postSideEffect(); 
        String elsex = elseoption.postSideEffect();
          
        if (condx == null && ifx == null && elsex == null) 
        { return null; }  
        
        String res = ""; 
        SequenceStatement stat = new SequenceStatement(); 

        if (condx != null) 
        { res = res + condx;
          if (cond.statement != null) 
          { stat.addStatement(cond.statement); }  
        } 

        if (ifx != null)
        { if (res.length() > 0)
          { res = res + " ; " + ifx; } 
          else 
          { res = ifx; } 
          if (ifoption.statement != null) 
          { stat.addStatement(ifoption.statement); }  
        } 

        if (elsex != null) 
        { if (res.length() > 0)
          { res = res + " ; " + elsex; } 
          else 
          { res = elsex; } 
          if (elseoption.statement != null) 
          { stat.addStatement(elseoption.statement); }  
        }  

        if (stat.getStatements().size() == 1) 
        { statement = (Statement) stat.getStatement(0); } 
        else 
        { statement = stat; } 

        return res; 
      } 
    } 
    return null; 
  } 

  public static boolean isCollectionsWrapperOp(String op)
  { Vector wops = new Vector(); 
    wops.add("synchronizedCollection"); 
    wops.add("synchronizedList"); 
    wops.add("synchronizedSet"); 
    wops.add("synchronizedMap"); 
    wops.add("synchronizedSortedSet"); 
    wops.add("synchronizedSortedMap"); 

    wops.add("unmodifiableCollection"); 
    wops.add("unmodifiableList"); 
    wops.add("unmodifiableSet"); 
    wops.add("unmodifiableMap"); 
    wops.add("unmodifiableSortedSet"); 
    wops.add("unmodifiableSortedMap"); 
    return wops.contains(op); 
  } 

  public String caseStatementstoKM3(String etest, Expression swtest, Vector terms, Vector statements)
  { // From position 3, a series of switchBlockStatementGroup.
    String res = ""; 

    Vector stats = new Vector();

    Vector remainingTerms = new Vector(); 
    remainingTerms.addAll(terms); 
    remainingTerms.remove(0); // switch
    remainingTerms.remove(0); // (parExpression ...)
    remainingTerms.remove(0); // {
    
    for (int i = 2; i < terms.size(); i++)
    { if (terms.get(i) instanceof ASTCompositeTerm)
      { ASTCompositeTerm sbsg = (ASTCompositeTerm) terms.get(i); 
        remainingTerms.remove(0); 
        stats.add(sbsg.switchBlocktoKM3(etest,swtest,remainingTerms));

        if (sbsg.statement != null) 
        { statements.add(sbsg.statement); }     
      } 
    } 

    for (int j = 0; j < stats.size(); j++) 
    { String stat = (String) stats.get(j); 
      res = res + stat;
      if (j < stats.size() - 1) 
      { res = res + ";\n"; }  
    }   

    return res;
  }  
     
  private String switchBlocktoKM3(String etest, Expression swtest, Vector followingBlocks)
  { String res = ""; 
    if ("switchBlockStatementGroup".equals(tag))
    { // (switchBlockStatementGroup (switchLabel ...)* (blockStatement ...)*
      // accumulate all the tests. 
      // Then accumulate the longest series of statements
      // without a break to the end of the switch
      // (followingBlocks). 

      Vector tests = new Vector(); 
      Vector statements = new Vector(); 

      Vector exprs = new Vector(); 
      Vector stats = new Vector(); 
      boolean hasBreak = false; 

      for (int i = 0; i < terms.size(); i++) 
      { ASTTerm cx = (ASTTerm) terms.get(i); 
        if ((cx instanceof ASTCompositeTerm) && 
            ((ASTCompositeTerm) cx).tag.equals("switchLabel") && 
             "case".equals(((ASTCompositeTerm) cx).terms.get(0) + ""))
        { ASTCompositeTerm slabel = (ASTCompositeTerm) cx; 
          ASTTerm sexpr = (ASTTerm) slabel.terms.get(1);
          String sval = sexpr.toKM3(); 
          tests.add("(" + etest + " = " + sval + ")"); 
          
          if (swtest != null && sexpr.expression != null) 
          { BinaryExpression texpr = 
              new BinaryExpression("=", swtest, sexpr.expression); 
            texpr.setBrackets(true); 
            exprs.add(texpr);  
          }
        }  
        else if ((cx instanceof ASTCompositeTerm) && 
                 ((ASTCompositeTerm) cx).tag.equals("blockStatement"))
        { statements.add(cx.toKM3()); 
          if (cx.statement != null) 
          { stats.add(cx.statement);
            if (cx.statement instanceof BreakStatement)
            { hasBreak = true; }
          } 
        } 
      }

      if (!hasBreak)
      { System.out.println(">>> Need to continue branch for " + tests + " into following statements: " + followingBlocks); 
        getContinuedSwitchCode(followingBlocks,statements,stats); 
      }


      String sequenceStatement = ""; 
      for (int j = 0; j < statements.size(); j++) 
      { String stat = (String) statements.get(j); 

        sequenceStatement = sequenceStatement + 
          "      " + stat; 
        if (j < statements.size() - 1)
        { sequenceStatement = sequenceStatement + " ;\n"; } 
        else 
        { sequenceStatement = sequenceStatement + "\n"; } 
      }

      SequenceStatement sq = new SequenceStatement(stats); 

      // if ("or" of tests) then statements else skip
      if (tests.size() > 0)
      { Expression condexpr = new BasicExpression(false); 

        String cond = ""; 
        for (int j = 0; j < tests.size(); j++) 
        { cond = cond + tests.get(j);
 
          if (j < tests.size() - 1) 
          { cond = cond + " or "; } 

          if (j < exprs.size())
          { Expression cexpr = (Expression) exprs.get(j); 
            condexpr = Expression.simplifyOr(condexpr,cexpr);
          }  
        }


        InvocationStatement skp = new InvocationStatement("skip"); 

        statement = new ConditionalStatement(condexpr, sq, skp); 

        return "    if " + cond + " then\n" + 
               sequenceStatement + 
               "    else skip ";  
      } 
      else 
      { statement = sq; 
        return sequenceStatement; 
      } 
    }
    return res;  
  }

  static void getContinuedSwitchCode(Vector followingBlocks, Vector statements, Vector stats)
  { for (int i = 0; i < followingBlocks.size(); i++) 
    { ASTTerm trm = (ASTTerm) followingBlocks.get(i); 
      if (trm instanceof ASTCompositeTerm)
      { ASTCompositeTerm sbsg = (ASTCompositeTerm) trm;
        if ("switchBlockStatementGroup".equals(sbsg.getTag())) 
        { boolean hasBreak = 
            getContinuedSwitchBlockCode(sbsg.terms, statements,stats);
          if (hasBreak) 
          { return; }
        }   
      } 
    } 
  }

  static boolean getContinuedSwitchBlockCode(Vector blockElements, Vector statements, Vector stats)
  { for (int i = 0; i < blockElements.size(); i++) 
    { ASTTerm trm = (ASTTerm) blockElements.get(i); 
      if (trm instanceof ASTCompositeTerm) 
      { ASTCompositeTerm sbelem = (ASTCompositeTerm) trm; 
        if ("switchLabel".equals(sbelem.getTag())) { } 
        else if ("blockStatement".equals(sbelem.getTag()))
        { String extraStat = sbelem.toKM3(); 
          statements.add(extraStat); 

          if (sbelem.statement != null) 
          { stats.add(sbelem.statement); } 
          if (sbelem.statement instanceof BreakStatement) 
          { return true; } // or return
        }  
      } 
    } 
    return false; 
  }
      

  public boolean isIdentifier()
  { if ("primary".equals(tag) && 
        terms.size() == 1)
    { ASTTerm t = (ASTTerm) terms.get(0); 
      String value = t.literalForm(); 
      if (value.length() > 0 && 
          Character.isJavaIdentifierStart(value.charAt(0)))
      { return true; } 
      return false; 
    } 
    
    if ("expression".equals(tag) && 
        terms.size() == 1)
    { ASTTerm t = (ASTTerm) terms.get(0); 
      return t.isIdentifier(); 
    }
 
    return false; 
  } 

} 