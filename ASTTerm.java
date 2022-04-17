/******************************
* Copyright (c) 2003--2022 Kevin Lano
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0
*
* SPDX-License-Identifier: EPL-2.0
* *****************************/

import java.util.Vector; 
import java.io.*; 

public abstract class ASTTerm
{ String id = ""; 
  // Vector stereotypes = new Vector(); 
  

  // A programming language AST can be represented by
  // one of the following UML/OCL things:
  
  Expression expression = null; 
  Statement statement = null; 
  ModelElement modelElement = null; 
  Vector modelElements = null;   
  Vector expressions = null; // for parameter/argument lists

  static String packageName = null; 
  static Vector enumtypes; 
  static Vector entities; 
  static Entity currentClass = null; // Current context class

  static java.util.Map metafeatures = new java.util.HashMap(); 
     // String --> Vector(String), 
     // eg., recording the conceptual
     // type of the element & stereotypes. 

  static java.util.Map types = new java.util.HashMap(); 
     // String --> String for general type of identifiers
     // valid at the scope of the current term. 

  static Vector cqueryfunctions = new Vector(); 
  static
  { cqueryfunctions.add("sin"); 
    cqueryfunctions.add("cos"); 
    cqueryfunctions.add("tan"); 
    cqueryfunctions.add("asin"); 
    cqueryfunctions.add("acos"); 
    cqueryfunctions.add("atan");
    cqueryfunctions.add("sinh"); 
    cqueryfunctions.add("cosh"); 
    cqueryfunctions.add("tanh"); 
    cqueryfunctions.add("exp"); 
    cqueryfunctions.add("log"); 
    cqueryfunctions.add("log10"); 
    cqueryfunctions.add("sqrt"); 
    cqueryfunctions.add("ceil"); 
    cqueryfunctions.add("floor"); 
    cqueryfunctions.add("fabs"); 
    cqueryfunctions.add("abs"); 
    cqueryfunctions.add("labs"); 
    cqueryfunctions.add("pow"); 
    cqueryfunctions.add("atan2"); 
    cqueryfunctions.add("ldexp"); 
    cqueryfunctions.add("fmod"); 
    cqueryfunctions.add("atof"); 
    cqueryfunctions.add("atoi"); 
    cqueryfunctions.add("atol"); 
    cqueryfunctions.add("isalnum"); 
    cqueryfunctions.add("isalpha"); 
    cqueryfunctions.add("isspace"); 
    cqueryfunctions.add("isdigit"); 
    cqueryfunctions.add("isupper");
    cqueryfunctions.add("islower"); 
    cqueryfunctions.add("iscntrl"); 
    cqueryfunctions.add("isgraph"); 
    cqueryfunctions.add("isprint"); 
    cqueryfunctions.add("ispunct"); 
    cqueryfunctions.add("isxdigit");
    cqueryfunctions.add("isnan");  
    cqueryfunctions.add("calloc"); 
    cqueryfunctions.add("malloc"); 
    // cqueryfunctions.add("realloc"); 
    cqueryfunctions.add("strcmp"); 
    cqueryfunctions.add("strncmp"); 
    cqueryfunctions.add("strchr"); 
    cqueryfunctions.add("strrchr"); 
    cqueryfunctions.add("strlen"); 
    cqueryfunctions.add("strstr"); 
    cqueryfunctions.add("strpsn"); 
    cqueryfunctions.add("strcpsn"); 
    cqueryfunctions.add("strpbrk"); 
    cqueryfunctions.add("strerror"); 
    cqueryfunctions.add("getenv"); 
    cqueryfunctions.add("bsearch"); 
    cqueryfunctions.add("toupper"); 
    cqueryfunctions.add("tolower");
    cqueryfunctions.add("fopen");
    cqueryfunctions.add("div"); 
    cqueryfunctions.add("ldiv");

    cqueryfunctions.add("time"); 
    cqueryfunctions.add("difftime"); 
    cqueryfunctions.add("localtime"); 
    cqueryfunctions.add("mktime"); 
    cqueryfunctions.add("asctime");
    cqueryfunctions.add("ctime"); 
    cqueryfunctions.add("gmtime"); 
  }

  public abstract String toString(); 

  public abstract String getTag(); 

  public abstract String literalForm();

  public abstract String tagFunction(); 

  public void addStereotype(String str) 
  { String lit = literalForm(); 
    Vector stereotypes = 
      (Vector) ASTTerm.metafeatures.get(lit); 
    if (stereotypes == null) 
    { stereotypes = new Vector(); 
      ASTTerm.metafeatures.put(lit,stereotypes); 
    } 

    if (stereotypes.contains(str)) {} 
    else 
    { stereotypes.add(str); } 
  } 

  public void removeStereotype(String str) 
  { String lit = literalForm(); 
    Vector stereotypes = 
      (Vector) ASTTerm.metafeatures.get(lit); 
    if (stereotypes == null) 
    { stereotypes = new Vector(); 
      ASTTerm.metafeatures.put(lit,stereotypes); 
    } 
    Vector removed = new Vector(); 
    removed.add(str); 
    stereotypes.removeAll(removed);  
  } 

  public boolean hasStereotype(String str) 
  { String lit = literalForm(); 
    Vector stereotypes = 
      (Vector) ASTTerm.metafeatures.get(lit); 
    if (stereotypes == null) 
    { stereotypes = new Vector(); 
      ASTTerm.metafeatures.put(lit,stereotypes); 
    } 
    return stereotypes.contains(str); 
  } 

  public abstract ASTTerm removeOuterTag(); 

  public abstract ASTTerm getTerm(int i); 

  public abstract Vector tokenSequence(); 

  public abstract int termSize(); 

  public abstract Vector getTerms(); 

  public abstract Vector symbolTerms(); 

  public abstract Vector nonSymbolTerms(); 

  public abstract String toJSON(); 

  public abstract String asTextModel(PrintWriter out); 

  public abstract String cg(CGSpec cgs); 

  public abstract String cgRules(CGSpec cgs, Vector rules); 

  // Only for programming languages. 
  public abstract boolean updatesObject(ASTTerm t); 

  public abstract boolean callSideEffect(); 

  // Only for programming languages. 
  public abstract boolean hasSideEffect(); 

  public abstract boolean isIdentifier(); 

  public abstract String preSideEffect(); 

  public abstract String postSideEffect(); 

  public boolean hasMetafeature(String f) 
  { String val = (String) metafeatures.get(f); 
    return val != null; 
  } 

  public void setMetafeature(String f, String val) 
  { metafeatures.put(f,val); } 

  public String getMetafeatureValue(String f) 
  { String val = (String) metafeatures.get(f); 
    return val;  
  } 


  public static void setType(ASTTerm t, String val) 
  { String f = t.literalForm(); 
    types.put(f,val); 
  } 

  public static void setType(String f, String val) 
  { types.put(f,val); } 

  public static String getType(String f) 
  { String val = (String) types.get(f); 
    return val;  
  } 

  public static String getType(ASTTerm t) 
  { String val = (String) types.get(t.literalForm());
    if (val == null && t instanceof ASTBasicTerm) 
    { ASTBasicTerm bt = (ASTBasicTerm) t; 
      return bt.getType(); 
    } 
    return val;  
  }

  public static String getElementType(ASTTerm t) 
  { String val = ASTTerm.getType(t);
    if (val != null)
    { Type typ = Type.getTypeFor(val, ASTTerm.enumtypes, ASTTerm.entities); 
      if (typ != null && typ.elementType != null) 
      { return typ.elementType + ""; } 
    } 
    return "OclAny";  
  }


  public boolean hasType(String str)
  { if ("character".equalsIgnoreCase(str))
    { return isCharacter(); } 
    if ("integer".equalsIgnoreCase(str))
    { return isInteger(); } 
    if ("real".equalsIgnoreCase(str))
    { return isReal(); } 
    if ("boolean".equalsIgnoreCase(str))
    { return isBoolean(); }
    if ("String".equalsIgnoreCase(str))
    { return isString(); }  

    if ("Sequence".equals(str))
    { return isSequence(); } 
    if ("StringSequence".equals(str))
    { return isStringSequence(); } 
    if ("IntegerSequence".equals(str))
    { return isIntegerSequence(); }
    if ("RealSequence".equals(str))
    { return isRealSequence(); }
    if ("BooleanSequence".equals(str))
    { return isBooleanSequence(); }
 
    if ("Set".equals(str))
    { return isSet(); } 
    if ("Map".equals(str))
    { return isMap(); } 
    if ("Function".equals(str))
    { return isFunction(); } 
    if ("Collection".equals(str))
    { return isCollection(); } 
    if ("File".equals(str))
    { return isFile(); } 
    if ("Date".equals(str))
    { return isDate(); } 
    if ("Process".equals(str))
    { return isProcess(); } 

    String typ = ASTTerm.getType(this);
    if (typ == null) 
    { return false; } 
 
    return typ.equals(str); 
  }  


  public abstract boolean isLabeledStatement();  

  public abstract String getLabel();  

  public abstract Type pointersToRefType(String tname, Type t);

  public static Entity introduceCStruct(String nme, Vector ents)
  { if (nme.equals("tm"))
    { Entity ent = 
        (Entity) ModelElement.lookupByName("OclDate", ents); 
      if (ent != null) 
      { return ent; }
      ent = new Entity("OclDate"); 
      ent.addStereotype("component"); 
      ent.addStereotype("external"); 
      ents.add(ents.size()-1, ent); 
      return ent; 
    } 

    Entity ent = (Entity) ModelElement.lookupByName(nme, ents); 
    if (ent != null) 
    { return ent; } 

    ent = new Entity(nme); 
    ent.addStereotype("struct"); 
    ents.add(ents.size()-1, ent); 

    if ("div_t".equals(nme))
    { Attribute quot = 
        new Attribute("quot", new Type("int", null), 
                      ModelElement.INTERNAL); 
      ent.addAttribute(quot); 
      Attribute rem = 
        new Attribute("rem", new Type("int", null), 
                      ModelElement.INTERNAL); 
      ent.addAttribute(rem); 
    } 
    else if ("ldiv_t".equals(nme))
    { Attribute quot = 
        new Attribute("quot", new Type("long", null), 
                      ModelElement.INTERNAL); 
      ent.addAttribute(quot); 
      Attribute rem = 
        new Attribute("rem", new Type("long", null), 
                      ModelElement.INTERNAL); 
      ent.addAttribute(rem); 
    } 
    /* else if ("tm".equals(nme))
    { Attribute secs = 
        new Attribute("tm_sec", new Type("int", null), 
                      ModelElement.INTERNAL); 
      ent.addAttribute(secs); 
      Attribute mins = 
        new Attribute("tm_min", new Type("int", null), 
                      ModelElement.INTERNAL); 
      ent.addAttribute(mins); 
      Attribute hrs = 
        new Attribute("tm_hour", new Type("int", null), 
                      ModelElement.INTERNAL); 
      ent.addAttribute(hrs); 
      Attribute days = 
        new Attribute("tm_mday", new Type("int", null), 
                      ModelElement.INTERNAL); 
      ent.addAttribute(days); 
      Attribute months = 
        new Attribute("tm_mon", new Type("int", null), 
                      ModelElement.INTERNAL); 
      ent.addAttribute(months);
      Attribute years = 
        new Attribute("tm_year", new Type("int", null), 
                      ModelElement.INTERNAL); 
      ent.addAttribute(years); 
      Attribute wday = 
        new Attribute("tm_wday", new Type("int", null), 
                      ModelElement.INTERNAL); 
      ent.addAttribute(wday);
      Attribute yday = 
        new Attribute("tm_yday", new Type("int", null), 
                      ModelElement.INTERNAL); 
      ent.addAttribute(yday); 
      Attribute isdst = 
        new Attribute("tm_isdst", new Type("int", null), 
                      ModelElement.INTERNAL); 
      ent.addAttribute(isdst);  
    } */ 

    return ent; 
  }

     
  public abstract Type cdeclarationToType(java.util.Map vartypes, 
    java.util.Map varelemtypes, Vector types, Vector entities);

  public abstract ModelElement cdeclaratorToModelElement(java.util.Map vartypes, 
    java.util.Map varelemtypes, Vector types, Vector entities);

  public abstract Vector cdeclaratorToModelElements(java.util.Map vartypes, 
    java.util.Map varelemtypes, Vector types, Vector entities);

  public abstract Vector cparameterListToKM3(java.util.Map vartypes, 
    java.util.Map varelemtypes, Vector types, Vector entities);

  public abstract Attribute cparameterToKM3(java.util.Map vartypes, 
    java.util.Map varelemtypes, Vector types, Vector entities);

  public abstract Statement cstatementToKM3(java.util.Map vartypes, java.util.Map varelemtypes, Vector types, Vector entities); 

  public abstract Statement cupdateForm(java.util.Map vartypes, java.util.Map varelemtypes, Vector types, Vector entities); 

  public abstract Statement cbasicUpdateForm(java.util.Map vartypes, java.util.Map varelemtypes, Vector types, Vector entities); 

  public abstract Statement cpreSideEffect(java.util.Map vartypes, java.util.Map varelemtypes, Vector types, Vector entities); 

  public abstract Statement cpostSideEffect(java.util.Map vartypes, java.util.Map varelemtypes, Vector types, Vector entities); 

  public abstract Vector cstatementListToKM3(java.util.Map vartypes, java.util.Map varelemtypes, Vector types, Vector entities); 

  public static boolean cqueryFunction(String fname)
  { return cqueryfunctions.contains(fname); } 

  public abstract Vector cexpressionListToKM3(java.util.Map vartypes, 
    java.util.Map varelemtypes, Vector types, Vector entities);

  public abstract Expression cexpressionToKM3(java.util.Map vartypes, java.util.Map varelemtypes, Vector types, Vector entities); 
  

  /* JavaScript abstraction: */ 

  public abstract Expression jsexpressionToKM3(java.util.Map vartypes, 
    java.util.Map varelemtypes, Vector types, Vector ents); 


  /* Java abstraction: */ 

  public abstract String queryForm(); 

  public abstract String toKM3(); 

  public boolean isAssignment() 
  { return false; } 

  public String toKM3Assignment()
  { return toKM3(); } 

  public static boolean isInteger(String typ) 
  { return 
      "int".equals(typ) || "short".equals(typ) || 
      "byte".equals(typ) || "Integer".equals(typ) || 
      "Short".equals(typ) || "Byte".equals(typ) ||
      "BigInteger".equals(typ) || "integer".equals(typ) ||  
      "long".equals(typ) || "Long".equals(typ); 
  } 

  public static boolean isReal(String typ) 
  { return 
      "float".equals(typ) || "double".equals(typ) || 
      "BigDecimal".equals(typ) || "real".equals(typ) || 
      "Float".equals(typ) || "Double".equals(typ); 
  } 

  public static boolean isString(String typ) 
  { return 
      "String".equals(typ) || "Character".equals(typ) || 
      "StringBuffer".equals(typ) || "char".equals(typ) || 
      "StringBuilder".equals(typ); 
  } 

  public static boolean isBoolean(String typ) 
  { return 
      "boolean".equals(typ) || "Boolean".equals(typ); 
  } 

  public boolean isString() 
  { String litf = literalForm(); 
    String typ = ASTTerm.getType(litf); 
    if (typ == null) 
    { return Expression.isString(litf); } 
    return 
      "String".equals(typ) || "Character".equals(typ) || 
      "StringBuffer".equals(typ) || "char".equals(typ) || 
      "StringBuilder".equals(typ); 
  } 

  public boolean isCharacter()
  { String s = literalForm(); 
    if (s.length() > 2 && s.startsWith("'") && 
        s.endsWith("'"))
    { return true; } 
    return false; 
  } 

  public boolean isInteger() 
  { String litf = literalForm(); 
    String typ = ASTTerm.getType(litf);
    if (typ == null)
    { return Expression.isInteger(litf) ||
             Expression.isLong(litf); 
    }   
    return ASTTerm.isInteger(typ); 
  } 

  public boolean isReal() 
  { String litf = literalForm(); 
    String typ = ASTTerm.getType(litf); 
    if (typ == null) 
    { return Expression.isDouble(litf); } 
    return ASTTerm.isReal(typ); 
  } 

  public boolean isNumber() 
  { String litf = literalForm(); 
    String typ = ASTTerm.getType(litf);
    if (typ == null) 
    { return Expression.isInteger(litf) ||
             Expression.isLong(litf) || 
             Expression.isDouble(litf); 
    } 
 
    return ASTTerm.isReal(typ) || ASTTerm.isInteger(typ); 
  } 

  public boolean isBoolean() 
  { String litf = literalForm(); 
    String typ = ASTTerm.getType(litf); 
    if (typ == null) 
    { return "true".equalsIgnoreCase(litf) || 
             "false".equalsIgnoreCase(litf); 
    } 
    return 
      "boolean".equals(typ) || "Boolean".equals(typ); 
  } 

  public boolean isCollection()
  { String typ = ASTTerm.getType(literalForm()); 
    if (typ == null) 
    { return false; } 
    if ("Sequence".equals(typ) || "Set".equals(typ) ||
        typ.startsWith("Sequence(") || typ.startsWith("Set("))
    { return true; } 
    return false; 
  } 

  public boolean isSet()
  { String typ = ASTTerm.getType(literalForm()); 
    if (typ == null) 
    { return false; } 
    if ("Set".equals(typ) || typ.startsWith("Set("))
    { return true; } 
    return false; 
  } 

  public boolean isSequence()
  { String typ = ASTTerm.getType(literalForm()); 
    if (typ == null) 
    { return false; } 
    if ("Sequence".equals(typ) || typ.startsWith("Sequence("))
    { return true; } 
    return false; 
  } 

  public boolean isMap()
  { String typ = ASTTerm.getType(literalForm()); 
    if (typ == null) 
    { return false; } 
    if ("Map".equals(typ) || typ.startsWith("Map("))
    { return true; } 
    return false; 
  } 

  public boolean isFunction()
  { String typ = ASTTerm.getType(literalForm()); 
    if (typ == null) 
    { return false; } 
    if ("Function".equals(typ) || typ.startsWith("Function("))
    { return true; } 
    return false; 
  } 

  public boolean isDate()
  { String typ = ASTTerm.getType(literalForm()); 
    if (typ == null) 
    { return false; } 
    if ("OclDate".equals(typ))
    { return true; } 
    return false; 
  } 

  public boolean isFile()
  { String typ = ASTTerm.getType(literalForm()); 
    if (typ == null) 
    { return false; } 
    if ("OclFile".equals(typ))
    { return true; } 
    return false; 
  } 

  public boolean isOclFile()
  { return isFile(); } 

  public boolean isProcess()
  { String typ = ASTTerm.getType(literalForm()); 
    if (typ == null) 
    { return false; } 
    if ("OclProcess".equals(typ))
    { return true; } 
    return false; 
  } 

  public boolean isStringSequence()
  { String typ = ASTTerm.getType(literalForm()); 
    if (typ == null) 
    { return false; } 
    if (typ.equals("Sequence(String)"))
    { return true; } 
    return false; 
  } 

  public boolean isIntegerSequence()
  { String typ = ASTTerm.getType(literalForm()); 
    if (typ == null) 
    { return false; } 
    if (typ.equals("Sequence(int)"))
    { return true; } 
    return false; 
  } 

  public boolean isRealSequence()
  { return isDoubleSequence(); } 

  public boolean isDoubleSequence()
  { String typ = ASTTerm.getType(literalForm()); 
    if (typ == null) 
    { return false; } 
    if (typ.equals("Sequence(double)"))
    { return true; } 
    return false; 
  } 

  public boolean isBooleanSequence()
  { String typ = ASTTerm.getType(literalForm()); 
    if (typ == null) 
    { return false; } 
    if (typ.equals("Sequence(boolean)"))
    { return true; } 
    return false; 
  } 

  public boolean isBitSet()
  { return isBooleanSequence(); } 
  // and original jtype is "BitSet". 

  public String getDefaultValue(String typ) 
  { if (isInteger(typ))
    { return "0"; } 
    if (isReal(typ))
    { return "0.0"; } 
    if (isString(typ))
    { return "\"\""; } 
    if (isBoolean(typ))
    { return "false"; } 
    
    return "null"; 
  } 

  public Expression getExpression()
  { return expression; } 

  public Statement getStatement()
  { return statement; } 

  public Entity getEntity()
  { if (modelElement != null && modelElement instanceof Entity)
    { return (Entity) modelElement; }
    return null; 
  }  

  public abstract boolean hasTag(String tagx); 

  public abstract boolean hasSingleTerm(); 

  public abstract int arity(); 

  public abstract int nonSymbolArity(); 

  public static ASTTerm removeFirstTerm(ASTTerm t)
  { if (t instanceof ASTCompositeTerm)
    { ASTCompositeTerm tt = (ASTCompositeTerm) t; 
      Vector newterms = new Vector(); 
      newterms.addAll(tt.getTerms()); 
      newterms.remove(0); 
      return new ASTCompositeTerm(tt.getTag(), newterms); 
    }
    else if (t instanceof ASTBasicTerm)
    { return new ASTCompositeTerm(t.getTag()); }  
    return t; 
  } 

  public static boolean constantTrees(ASTTerm[] trees)
  { if (trees.length == 0) 
    { return false; }
    ASTTerm t0 = trees[0]; 
    for (int i = 1; i < trees.length; i++) 
    { ASTTerm tx = trees[i]; 
	  if (tx == null) 
	  { return false; }
      if (tx.equals(t0)) { } 
      else 
      { return false; } 
    } 
    return true; 
  }   

  public static boolean allSymbolTerms(ASTTerm[] trees)
  { if (trees.length == 0) 
    { return false; }
    for (int i = 0; i < trees.length; i++) 
    { ASTTerm tx = trees[i]; 
      if (tx instanceof ASTSymbolTerm) { } 
      else 
      { return false; } 
    } 
    return true; 
  }   

  public static boolean allNestedSymbolTerms(ASTTerm[] trees)
  { // All either symbols or nested symbols

    if (trees.length == 0) 
    { return false; }

    for (int i = 0; i < trees.length; i++) 
    { ASTTerm tx = trees[i]; 
      if (tx == null) 
      { return false; } 

      if (tx.arity() <= 1 && tx.isNestedSymbolTerm()) 
      { 
        System.out.println(">>> Nested symbol term: " + tx); 
      } 
      else 
      { return false; }  
    } 
    return true; 
  }   

  public abstract boolean isNestedSymbolTerm(); 
  
  public static boolean recursivelyNestedEqual(
      ASTTerm[] strees, ASTTerm[] ttrees)
  { // Each strees[i] is a symbol, literally equal 
    // to the targets

    if (strees.length == 0) 
    { return false; }

    if (strees.length != ttrees.length) 
    { return false; } 

    for (int i = 0; i < strees.length; i++) 
    { ASTTerm sx = strees[i];
      ASTTerm tx = ttrees[i]; 
 
      if (sx == null || tx == null) 
      { return false; } 

      String slit = sx.literalForm(); 
      String tlit = tx.literalForm(); 
      
      if (slit.equals(tlit)) { } 
      else 
      { return false; } 
    } 

    return true; 
  }   


  public static boolean functionalSymbolMapping(ASTTerm[] strees, ASTTerm[] ttrees)
  { // The correspondence is functional. Not only symbols. 

    String[] sattvalues = new String[strees.length]; 
    String[] tattvalues = new String[ttrees.length]; 

    for (int i = 0; i < strees.length; i++) 
    { if (strees[i] == null) 
      { return false; } 
      sattvalues[i] = strees[i].literalForm(); 
    } 

    for (int i = 0; i < ttrees.length; i++) 
    { if (ttrees[i] == null) 
      { return false; } 
      tattvalues[i] = ttrees[i].literalForm(); 
    } 
 
    return AuxMath.isFunctional(sattvalues,tattvalues); 
  } 

  public static boolean functionalASTMapping(Vector strees, Vector values)
  { // The correspondence is functional. Not only symbols. 

    String[] sattvalues = new String[strees.size()]; 
    String[] tattvalues = new String[values.size()]; 

    for (int i = 0; i < strees.size(); i++) 
    { ASTTerm sterm = (ASTTerm) strees.get(i); 
      if (sterm == null) 
      { return false; } 
      sattvalues[i] = sterm.literalForm(); 
    } 

    for (int i = 0; i < values.size(); i++) 
    { String ts = (String) values.get(i); 
      if (ts == null) 
      { return false; } 
      tattvalues[i] = ts; 
    } 
 
    return AuxMath.isFunctional(sattvalues,tattvalues); 
  } 

  public static boolean functionalNestedSymbolMapping(ASTTerm[] strees, ASTTerm[] ttrees)
  { // The correspondence of strees single element & 
    // target terms is functional.

    String[] sattvalues = new String[strees.length]; 
    String[] tattvalues = new String[ttrees.length]; 

    for (int i = 0; i < strees.length; i++) 
    { if (strees[i] == null) 
      { return false; } 
      sattvalues[i] = strees[i].literalForm(); 
    } 

    for (int i = 0; i < ttrees.length; i++) 
    { if (ttrees[i] == null) 
      { return false; } 
      tattvalues[i] = ttrees[i].literalForm(); 
    } 
 
    return AuxMath.isFunctional(sattvalues,tattvalues); 
  } 

  public static TypeMatching createNewFunctionalMapping(String name, ASTTerm[] strees, ASTTerm[] ttrees)
  { // The correspondence is functional.

    // If an identity mapping, return _1 |-->_1

    // A "default" case _* |--> v is included where v is the 
    // most frequent target of different source values. 

    Type str = new Type("String", null); 
    TypeMatching tm = new TypeMatching(str,str);
    tm.setName(name);  

    String[] sattvalues = new String[strees.length]; 
    String[] tattvalues = new String[ttrees.length]; 

    for (int i = 0; i < strees.length; i++) 
    { sattvalues[i] = strees[i].literalForm(); } 

    for (int i = 0; i < ttrees.length; i++) 
    { tattvalues[i] = ttrees[i].literalForm(); } 

    if (AuxMath.isIdentity(sattvalues,tattvalues))
    { tm.addDefaultMapping("_1", "_1"); 
      return tm; 
    }

    /* Identify default mapping _1 |--> v */ 

    String defaultValue = null; 
    int defaultCount = 0; 
    for (int i = 0; i < tattvalues.length; i++) 
    { String tval = tattvalues[i]; 
      java.util.HashSet svals = new java.util.HashSet(); 
      for (int j = 0; j < sattvalues.length && 
                      j < tattvalues.length; j++) 
      { if (tattvalues[j].equals(tval))
        { svals.add(sattvalues[j]); } 
      } 

      if (svals.size() > defaultCount)
      { defaultValue = tval;
        defaultCount = svals.size();
      }     
    } 

    System.out.println(">> Default mapping is _* |-->" + defaultValue); 


    tm.setStringValues(sattvalues,tattvalues);

    if (defaultValue != null) 
    { tm.addDefaultMapping("_*", defaultValue); } 
 
    return tm; 
  } 

  public static boolean hasNullTerm(Vector strees)
  { for (int i = 0; i < strees.size(); i++) 
    { ASTTerm st = (ASTTerm) strees.get(i); 
      if (st == null) 
      { System.err.println("!WARNING: null element in " + strees + " -- INVALID AST DATA"); 
        return true; 
      } 
    } 
    return false; 
  } 
    
  public static boolean functionalTermMapping(Vector strees, Vector ttrees)
  { // The correspondence is functional.
    String[] sattvalues = new String[strees.size()]; 
    String[] tattvalues = new String[ttrees.size()]; 

    for (int i = 0; i < strees.size(); i++) 
    { ASTTerm st = (ASTTerm) strees.get(i); 
      if (st == null) 
      { System.err.println("!WARNING: null element in " + strees + " -- INVALID AST DATA"); 
        return false; 
      } 
      sattvalues[i] = st.literalForm(); 
    } 

    for (int i = 0; i < ttrees.size(); i++) 
    { ASTTerm tt = (ASTTerm) ttrees.get(i); 
      if (tt == null) 
      { System.err.println("!WARNING: null element in " + ttrees + " -- INVALID AST DATA"); 
        return false; 
      }
      tattvalues[i] = tt.literalForm(); 
    } 
 
    return AuxMath.isFunctional(sattvalues,tattvalues); 
  } 


  public static String commonTag(ASTTerm[] trees)
  { if (trees == null || trees.length == 0) 
    { return null; }
    ASTTerm t0 = trees[0]; 
    if (t0 == null) { return null; }
	
    if (t0 instanceof ASTBasicTerm) 
    { return ((ASTBasicTerm) t0).getTag(); } 
    else if (t0 instanceof ASTCompositeTerm) 
    { return ((ASTCompositeTerm) t0).getTag(); } 
    else 
    { return null; } 
  }

  public static boolean sameTag(ASTTerm[] trees)
  { if (trees == null || trees.length == 0) 
    { return false; }
    String tag0 = ""; 
    ASTTerm t0 = trees[0]; 
    if (t0 == null) { return false; }
	
    if (t0 instanceof ASTBasicTerm) 
    { tag0 = ((ASTBasicTerm) t0).getTag(); } 
    else if (t0 instanceof ASTCompositeTerm) 
    { tag0 = ((ASTCompositeTerm) t0).getTag(); } 
    else 
    { return false; } 

    for (int i = 1; i < trees.length; i++) 
    { ASTTerm tx = trees[i]; 
      if (tx.hasTag(tag0)) { } 
      else 
      { return false; } 
    } 
    return true; 
  }   

  public static boolean sameTails(ASTTerm[] trees)
  { if (trees.length == 0) 
    { return false; }
    Vector tail0 = new Vector(); 
    ASTTerm t0 = trees[0]; 

    if (t0 instanceof ASTCompositeTerm) 
    { Vector trms = new Vector(); 
      trms.addAll(((ASTCompositeTerm) t0).getTerms());
      if (trms.size() == 0) 
      { return false; } 
      trms.remove(0); 
      tail0.addAll(trms);  
    } 
    else 
    { return false; } 

    for (int i = 1; i < trees.length; i++) 
    { ASTTerm tx = trees[i]; 
      if (tx instanceof ASTCompositeTerm) 
      { Vector trms = new Vector(); 
        trms.addAll(((ASTCompositeTerm) tx).getTerms());
        if (trms.size() == 0) 
        { return false; } 
        trms.remove(0); 
        if (tail0.equals(trms)) { } 
        else 
        { return false; } 
      } 
      else 
      { return false; } 
    } 
    return true; 
  }   

  public static boolean sameTagSingleArgument(ASTTerm[] trees)
  { if (trees == null || trees.length == 0) 
    { return false; }
    String tag0 = ""; 
    ASTTerm t0 = trees[0]; 
    if (t0 == null) 
    { return false;} 

    if (t0 instanceof ASTBasicTerm) 
    { tag0 = ((ASTBasicTerm) t0).getTag(); } 
    else if (t0 instanceof ASTCompositeTerm && 
             t0.hasSingleTerm()) 
    { tag0 = ((ASTCompositeTerm) t0).getTag(); } 
    else 
    { return false; } 

    for (int i = 1; i < trees.length; i++) 
    { ASTTerm tx = trees[i]; 
      if (tx == null) 
      { return false; } 
      else if (tx.hasTag(tag0) && tx.hasSingleTerm()) { } 
      else 
      { return false; } 
    } 
    return true; 
  }   

  public static boolean sameTagSameArity(ASTTerm[] trees)
  { if (trees == null || trees.length == 0) 
    { return false; }
    String tag0 = ""; 
    ASTTerm t0 = trees[0];
    if (t0 == null) 
    { return false;} 
 
    int arit = t0.arity(); 

    if (t0 instanceof ASTBasicTerm) 
    { tag0 = ((ASTBasicTerm) t0).getTag(); } 
    else if (t0 instanceof ASTCompositeTerm) 
    { tag0 = ((ASTCompositeTerm) t0).getTag(); } 
    else 
    { return false; } 

    for (int i = 1; i < trees.length; i++) 
    { ASTTerm tx = trees[i]; 
      if (tx == null) 
      { return false; } 
      else if (tx.hasTag(tag0) && tx.arity() == arit) { } 
      else 
      { return false; } 
    } 
    return true; 
  }   

  public static boolean alwaysSymbol(int ind, ASTTerm[] trees)
  { if (trees == null || trees.length == 0) 
    { return false; }
    
    for (int i = 1; i < trees.length; i++) 
    { ASTTerm tx = trees[i]; 
      if (tx == null || ind >= tx.arity()) 
      { return false; } 
      ASTTerm tsx = tx.getTerm(ind); 
      if (tsx instanceof ASTSymbolTerm) { } 
      else 
      { return false; } 
    } 
      
    return true; 
  } 

  public static boolean allCompositeSameLength(
                                   ASTTerm[] trees)
  { if (trees == null || trees.length == 0) 
    { return false; }

    if (trees[0] == null) 
    { return false; } 

    int len0 = trees[0].arity(); 

    if (len0 <= 1) 
    { return false; } 
    
    for (int i = 1; i < trees.length; i++) 
    { ASTTerm tx = trees[i]; 
      if (tx == null || tx.arity() != len0) 
      { return false; } 
    } 
      
    return true; 
  } 

  public static Vector symbolValues(int ind, ASTTerm[] trees)
  { Vector res = new Vector(); 

    if (trees == null || trees.length == 0) 
    { return res; }
    
    for (int i = 1; i < trees.length; i++) 
    { ASTTerm tx = trees[i]; 
      if (tx == null || ind >= tx.arity()) 
      { continue; } 
      ASTTerm tsx = tx.getTerm(ind); 
      if (tsx instanceof ASTSymbolTerm) 
      { res.add(tsx.literalForm()); } 
    } 
      
    return res; 
  } 

  public static boolean hasTermValue(ASTTerm trm, int ind, String val) 
  { if (trm == null) 
    { return false; } 

    if (ind >= trm.arity())
    { return false; } 

    ASTTerm tsx = trm.getTerm(ind); 
    if (val.equals(tsx.literalForm()))
    { return true; } 
    return false; 
  } 
 

  public static ASTTerm[] removeOuterTag(ASTTerm[] trees, Vector remd)
  { if (trees == null || trees.length == 0) 
    { return trees; }

    ASTTerm[] result = new ASTTerm[trees.length]; 

    for (int i = 0; i < trees.length; i++) 
    { ASTTerm tx = trees[i];
      if (tx == null) 
      { remd.add(null); } 
      else  
      { ASTTerm tnew = tx.removeOuterTag(); 
        result[i] = tnew; 
        remd.add(tnew); 
      } 
    } 
    return result; 
  }   

  public static ASTTerm[] subterms(ASTTerm[] trees, int i, Vector remd)
  { if (trees == null || trees.length == 0) 
    { return trees; }

    ASTTerm[] result = new ASTTerm[trees.length]; 

    for (int j = 0; j < trees.length; j++) 
    { ASTTerm tx = trees[j]; 
      if (tx == null)
      { result[j] = null; 
        remd.add(null);
        // return null?  
      }
      else 
      { ASTTerm tnew = tx.getTerm(i); 
        result[j] = tnew; 
        remd.add(tnew);
      }  
    } // terms are indexed from 0

    return result; 
  }   

 public static boolean equalTrees(ASTTerm[] xs, ASTTerm[] ys, ModelSpecification mod)
    { // Is each ys[i] = xs[i]? 

      if (ys.length > 1 && xs.length == ys.length)
      { for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yvect = ys[i]; 

          System.out.println(">>>> Comparing " + xx + " to " + yvect); 
          if (xx == null) 
          { return false; } 
          else if (xx.equals(yvect)) { } 
          else  
          { return false; }
        }
        return true; 
      } 
      return false; 
    }

    public static Vector createIdentityMappings
        (ASTTerm[] xs, ASTTerm[] ys, ModelSpecification mod)
    { // Each ys[i] = xs[i] 
      // Create a separate mapping for each different arity
      // of terms. Look for constant symbol values. 

      Vector res = new Vector(); 
      java.util.Map aritymap = new java.util.HashMap(); 

      if (ys.length > 1 && xs.length == ys.length)
      { for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yvect = ys[i]; 

          if (xx != null) 
          { int n = xx.arity(); 
            Vector aritynterms = 
               (Vector) aritymap.get(n); 
            if (aritynterms == null) 
            { aritynterms = new Vector(); 
              aritynterms.add(xx);
              aritymap.put(n,aritynterms);  
              BasicExpression expr = 
                BasicExpression.newASTBasicExpression(xx); 
              AttributeMatching am = 
                new AttributeMatching(expr,expr); 
              res.add(am); 
            }
            else 
            { aritynterms.add(xx); }  
          } 
        }
      } 
      return res; 
    }

    public static Vector createGeneralisedMappings
        (ASTTerm[] xs, Expression trg)
    { // Each xs[i] maps to same target. 
      // Create a separate mapping for each different arity
      // of xs. Extract constant symbol values. 

      Vector res = new Vector(); 
      java.util.Map aritymap = new java.util.HashMap(); 
      Vector doms = new Vector(); 

      if (xs.length > 1)
      { for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
         
          if (xx != null) 
          { int n = xx.arity(); 
            Integer nx = new Integer(n); 
            doms.add(nx); 
            Vector aritynterms = 
               (Vector) aritymap.get(nx); 
            if (aritynterms == null) 
            { aritynterms = new Vector(); 
              aritynterms.add(xx);
              aritymap.put(nx,aritynterms);  
            }
            else 
            { aritynterms.add(xx); }  
          } 
        }
      } 
      else 
      { return res; } 

      for (int j = 0; j < doms.size(); j++) 
      { // For each arity set aritymap(doms(j))
        // identify the generalised form of the terms

        Integer nx = (Integer) doms.get(j); 
        Vector arityns = (Vector) aritymap.get(nx); 

        System.out.println(">>> Arity " + nx + " source terms are: " + arityns); 
        System.out.println(); 

        if (arityns.size() == 1) 
        { ASTTerm st = (ASTTerm) arityns.get(0); 
          BasicExpression expr = 
                BasicExpression.newASTBasicExpression(st); 
          AttributeMatching am = 
                new AttributeMatching(expr,trg); 
          res.add(am); 
        } 
        else 
        { ASTTerm st0 = (ASTTerm) arityns.get(0); 
          BasicExpression expr = 
                BasicExpression.newASTBasicExpression(st0); 
          int n = nx.intValue(); 
          for (int p = 0; p < n; p++) 
          { if (ASTTerm.constantTerms(arityns,p))
            { ASTTerm pterm = st0.getTerm(p); 
              String st0p = pterm.literalForm(); 
              Expression exprp = 
                      new BasicExpression(st0p);                    
              expr.setParameter(p+1, exprp); 
            }
            else 
            { expr.setParameter(p+1,
                    new BasicExpression("_" + (p+1))); 
            } 
          } 
          AttributeMatching am = 
                new AttributeMatching(expr,trg); 
          res.add(am); 
        }
      }  

      System.out.println(">>> Generalised matchings: " + res); 

      return res; 
    }

    public static boolean constantTerms(Vector trms, int p)
    { // The p subterms of all trms are the same 

      if (trms.size() == 0) 
      { return false; } 
   
      if (trms.size() == 1) 
      { return true; } 

      ASTTerm t0 = (ASTTerm) trms.get(0); 
      if (t0.arity() <= p) 
      { return false; } 

      ASTTerm subtermp = t0.getTerm(p);
 
      String lit = subtermp.literalForm(); 
      
      for (int i = 1; i < trms.size(); i++) 
      { ASTTerm t = (ASTTerm) trms.get(i); 
        ASTTerm subterm = t.getTerm(p);

        // System.out.println(" " + p + " subterm of " + t + " is " + subterm + " =? " + lit); 
 
        if (subterm != null && 
            subterm.literalForm().equals(lit))
        { } 
        else 
        { return false; } 
      } 

      // System.out.println(" " + p + " subterm is constant"); 

      return true; 
    } 

 /*   public static boolean matchingTrees(ASTTerm[] xs, ASTTerm[] ys, ModelSpecification mod)
    { // Is each ys[i] = xs[i], or corresponding under mod? 

      if (ys.length > 1 && xs.length == ys.length)
      { for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yvect = ys[i]; 

          System.out.println(">>>> Comparing " + xx + " to " + yvect); 
          if (xx == null) 
          { return false; } 
          else if (xx.equals(yvect)) { } 
          else if (mod.correspondingTrees(xx,yvect)) { } 
          else  
          { return false; }
        }
        return true; 
      } 
      return false; 
    } */ 

    public static boolean matchingTrees(Entity sent, ASTTerm[] xs, ASTTerm[] ys, ModelSpecification mod)
    { // Is each ys[i] = xs[i], or corresponding under mod? 

      if (ys.length > 1 && xs.length == ys.length)
      { for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yvect = ys[i]; 

          System.out.println(">>>> Comparing " + xx + " to " + yvect); 
          if (xx == null) 
          { return false; } 
          else if (xx.equals(yvect)) { } 
          else if (mod.correspondingTrees(sent, xx, yvect)) { } 
          else  
          { return false; }
        }
        return true; 
      } 
      return false; 
    }

    public static boolean matchingTrees(Entity sent, ASTTerm xx, ASTTerm yy, ModelSpecification mod)
    { System.out.println(">>>> Comparing " + xx + " to " + yy); 
      if (xx == null) 
      { return false; } 
      else if (xx.equals(yy)) 
      { return true; } 
      else if (mod.correspondingTrees(sent, xx, yy)) 
      { return true; } 
      return false; 
    }

 /* 
    
  public static boolean singletonTrees(ASTTerm[] xs, ASTTerm[] ys, ModelSpecification mod)
    { // Is each ys[i] = (tag xs[i]') for the same tag? 

      if (ys.length > 1 && xs.length == ys.length)
      { for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yvect = ys[i]; 

          if (xx == null || yvect == null)
          { return false; } 

          System.out.println(">>>> Comparing " + xx + " to " + yvect); 

          if (yvect instanceof ASTBasicTerm && 
              xx instanceof ASTSymbolTerm) 
          { String yy = ((ASTBasicTerm) yvect).getValue();
            String xsym = ((ASTSymbolTerm) xx).getSymbol();  
            if (yy.equals(xsym)) { }
            else 
            { return false; }
          } 
          else if (yvect instanceof ASTCompositeTerm && 
                   ((ASTCompositeTerm) yvect).getTerms().size() == 1)
          { ASTCompositeTerm ct = (ASTCompositeTerm) yvect; 
            ASTTerm ct0 = (ASTTerm) ct.getTerms().get(0); 
            if (xx.equals(ct0) || 
                mod.correspondingTrees(xx,ct0)) { } 
            else 
            { return false; } 
          } 
          else 
          { return false; } 
        }
        return true; 
      } 
      return false; 
    } */ 

  public static boolean singletonTrees(Entity sent, ASTTerm[] xs, ASTTerm[] ys, ModelSpecification mod)
    { // Is each ys[i] = (tag xs[i]') for the same tag? 

      if (ys.length > 1 && xs.length == ys.length)
      { for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yvect = ys[i]; 

          if (xx == null || yvect == null)
          { return false; } 

          System.out.println(">>>> Comparing " + xx + " to " + yvect); 

          if (yvect instanceof ASTBasicTerm && 
              xx instanceof ASTSymbolTerm) 
          { String yy = ((ASTBasicTerm) yvect).getValue();
            String xsym = ((ASTSymbolTerm) xx).getSymbol();  
            if (yy.equals(xsym)) { }
            else 
            { return false; }
          } // or vice-versa
          else if (yvect instanceof ASTCompositeTerm && 
                   ((ASTCompositeTerm) yvect).getTerms().size() == 1)
          { ASTCompositeTerm ct = (ASTCompositeTerm) yvect; 
            ASTTerm ct0 = (ASTTerm) ct.getTerms().get(0); 
            if (xx.equals(ct0) || 
                mod.correspondingTrees(sent, xx, ct0)) 
            { System.out.println(">>> corresponding trees: " + 
                                 xx + " " + ct0); 
            } 
            else 
            { return false; } 
          } 
          else 
          { return false; } 
        }
        return true; 
      } 
      return false; 
    }


    public static boolean sameArityTrees(ASTTerm[] xs, ASTTerm[] ys)
    { // Is each ys[i].terms.size() == xs[i].terms.size()? 

      if (ys.length > 1 && xs.length == ys.length)
      { for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yvect = ys[i]; 

          if (xx == null || yvect == null)
          { return false; } 

          int n = xx.arity(); 
          int m = yvect.arity(); 
          if (n != m) 
          { return false; } 
        } 
        return true; 
      } 
      return false; 
    }

    public static boolean sameNonSymbolArity(ASTTerm[] xs, ASTTerm[] ys)
    { // Is number of non-symbols in ys[i].terms == 
      // number of non-symbols in xs[i].terms? 

      if (ys.length > 1 && xs.length == ys.length)
      { for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yvect = ys[i]; 

          if (xx == null || yvect == null)
          { return false; } 

          int n = xx.nonSymbolArity();
          System.out.println(">***> Non-symbol arity of " + xx + " = " + n); 
 
          int m = yvect.nonSymbolArity(); 
          System.out.println(">***> Non-symbol arity of " + yvect + " = " + m); 

          if (n != m) 
          { return false; } 
        } 
        return true; 
      } 
      return false; 
    }

    public static boolean lowerNonSymbolArity(ASTTerm[] xs, ASTTerm[] ys)
    { // Is number of non-symbols in ys[i].terms <= 
      // number of non-symbols in xs[i].terms? 

      if (ys.length > 1 && xs.length == ys.length)
      { for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yvect = ys[i]; 

          if (xx == null || yvect == null)
          { return false; } 

          int n = xx.nonSymbolArity();
          System.out.println(">*>*> Non-symbol arity of " + xx + " = " + n); 
 
          int m = yvect.nonSymbolArity(); 
          System.out.println(">*>*> Non-symbol arity of " + yvect + " = " + m); 

          if (n < m) 
          { return false; } 
        } 
        return true; 
      } 
      return false; 
    }

    public static Vector concatenateCorrespondingTermLists(
                              ASTTerm xx, 
                              ModelSpecification mod)
    { Vector xxterms = xx.getTerms();
      Vector concatxxterms = new Vector(); 
 
      for (int j = 0; j < xxterms.size(); j++) 
      { ASTTerm xj = (ASTTerm) xxterms.get(j); 
        ASTTerm tj = mod.getCorrespondingTree(xj);
 
        System.out.println(">>^^>> Corresponding tree of " + xj + " is " + tj); 
 
        if (tj != null) 
        { 
          Vector tjterms = tj.getTerms();
          concatxxterms.addAll(tjterms);
        } 
      }  
      return concatxxterms; 
    }        
    
    public static boolean treeconcatenations(ASTTerm[] xs, 
                        ASTTerm[] ys, ModelSpecification mod)
    { // Each ys[i].terms == concatenation of terms of the ti 
      // corresponding to the xs[i].terms

      System.out.println(">>> Checking concatenation of terms"); 

      if (ys.length > 1 && xs.length == ys.length)
      { for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yy = ys[i]; 

          if (xx == null || yy == null)
          { return false; } 

          Vector xxterms = xx.getTerms();
          Vector concatxxterms = new Vector(); 
 
          for (int j = 0; j < xxterms.size(); j++) 
          { ASTTerm xj = (ASTTerm) xxterms.get(j); 
            ASTTerm tj = mod.getCorrespondingTree(xj);
 
            System.out.println(">>^^>> Corresponding tree of " + xj + " is " + tj); 
 
            if (tj != null) 
            { 
              Vector tjterms = tj.getTerms();
              concatxxterms.addAll(tjterms);
            }
            else 
            { Vector subterms = 
                concatenateCorrespondingTermLists(xj,mod); 
              concatxxterms.addAll(subterms);
            } 
          }  
            
          System.out.println(">>> Concatenantion of terms = " + concatxxterms); 
          System.out.println(">>> Target terms = " + yy.getTerms()); 

          if (concatxxterms.equals(yy.getTerms())) { } 
          else 
          { return false; } 
        } 
        return true; 
      } 
      return false; 
    }

    public static Vector concatenationSubMapping(
                              ASTTerm xx, 
                              ModelSpecification mod)
    { Vector xxterms = xx.getTerms();
      Vector symbs = new Vector(); 
 
      for (int j = 0; j < xxterms.size(); j++) 
      { ASTTerm xj = (ASTTerm) xxterms.get(j); 
        // ASTTerm tj = mod.getCorrespondingTree(xj);
 
        // System.out.println(">>^^>> Corresponding tree of " + xj + " is " + tj); 

        if (xj instanceof ASTSymbolTerm)
        { String symb = xj.literalForm(); 
          if (symbs.contains(symb)) { } 
          else 
          { symbs.add(symb); } 
        } 
 
        /* if (tj != null) 
        { 
          Vector tjterms = tj.getTerms();
          concatxxterms.addAll(tjterms);
        } */ 
      }  
      return symbs; 
    }        

    public static AttributeMatching 
      concatenationTreeMapping(ASTTerm[] xs, 
                        ASTTerm[] ys, ModelSpecification mod,
                        Vector tms)
    { // Each ys[i].terms == concatenation of terms of the ti 
      // corresponding to the xs[i].terms

      Vector pars = new Vector(); 
      Vector tpars = new Vector(); 

      if (ys.length > 1 && xs.length == ys.length)
      { // for (int i = 0; i < xs.length; i++)
        // { 
          ASTTerm xx0 = xs[0]; 
          ASTTerm yy0 = ys[0]; 

          if (xx0 == null || yy0 == null)
          { return null; } 

          int n = xs[0].arity();
             // Maximum arity of any xs[i]

          for (int i = 0; i < xs.length; i++)
          { if (n < xs[i].arity())
            { n = xs[i].arity(); } 
          } 

          Vector[] xtermvectors = new Vector[n]; 
          String targetTag = ys[0].getTag(); 

          for (int i = 0; i < n; i++)
          { xtermvectors[i] = new Vector(); } 

          for (int i = 0; i < xs.length; i++)
          { ASTTerm xx = xs[i]; 
            ASTTerm yy = ys[i]; 

            if (xx == null || yy == null)
            { return null; } 

            Vector xxterms = xx.getTerms();
            Vector yyterms = yy.getTerms(); 
            // Vector tterms = new Vector(); 
 
            for (int j = 0; j < xxterms.size(); j++) 
            { ASTTerm xj = (ASTTerm) xxterms.get(j); 
              ASTTerm tj = mod.getCorrespondingTree(xj);
 
              // System.out.println(">>^^>> Corresponding tree of " + xj + " is " + tj); 
 
              if (xj instanceof ASTSymbolTerm) 
              { xtermvectors[j].add(xj); } 
              else if (tj != null) 
              { // if (tj.getTag().equals(targetTag))
                // { tterms.addAll(tj.getTerms()); }
                // else 
                // { return null; } 
              }  
              else // Not included in concatenation
              { xtermvectors[j].add(xj); } 
            }  
          }             

          Vector xxterms = xx0.getTerms();
            // xx0 is a typical example. 
          
          for (int j = 0; j < xxterms.size(); j++) 
          { ASTTerm xj = (ASTTerm) xxterms.get(j); 
            ASTTerm tj = mod.getCorrespondingTree(xj);
  
            if (xj instanceof ASTSymbolTerm)
            { if (AuxMath.isConstantSeq(xtermvectors[j]))
              { pars.add(
                  new BasicExpression(xj.literalForm())); 
              } 
              else 
              { pars.add(
                  new BasicExpression("_" + (j+1))); 
              } 
            } 
            else if (tj != null) 
            { pars.add(
                new BasicExpression("_" + (j+1))); 
              tpars.add(
                new BasicExpression("_" + (j+1))); 

              // Vector tjterms = tj.getTerms();
              // concatxxterms.addAll(tjterms);
            }
            else 
            { Vector deletedSymbols = 
                concatenationSubMapping(xj,mod); 
              String fid = 
                Identifier.nextIdentifier("concatruleset");
              TypeMatching tmnew = new TypeMatching(fid);

              for (int d = 0; d < deletedSymbols.size(); d++) 
              { String ds = (String) deletedSymbols.get(d); 
                tmnew.addValueMapping(ds, " "); 
              } 

              tmnew.addValueMapping("_0", "_0"); 
              tms.add(tmnew); 

              String subid = 
                Identifier.nextIdentifier("concatrule");
              TypeMatching tmsub = new TypeMatching(subid);
              tmsub.addValueMapping("_*", "_*`" + fid); 
              tms.add(tmsub); 

              BasicExpression subexpr = 
                new BasicExpression(subid); 
              subexpr.setUmlKind(Expression.FUNCTION);
              subexpr.addParameter(new BasicExpression("_" + (j+1)));

              pars.add(
                new BasicExpression("_" + (j+1))); 
              tpars.add(subexpr);  
            } // nested mapping. 
          }  
        // }     
          
        BasicExpression srcterm = 
          new BasicExpression(xs[0]); 
        srcterm.setParameters(pars); 

        BasicExpression trgterm = 
          new BasicExpression(ys[0]); 
        trgterm.setParameters(tpars); 

        return new AttributeMatching(srcterm,trgterm); 
      } 
      return null; 
    }

    public static boolean treesuffixes(ASTTerm[] xs, 
                        ASTTerm[] ys, ModelSpecification mod)
    { // Each ys[i].terms == concatenation of xs[i].terms
      // plus a constant suffix of terms. 

      System.out.println(">&>&> Checking suffix relation of trees"); 

      java.util.HashSet suffixes = new java.util.HashSet(); 

      if (ys.length > 1 && xs.length == ys.length)
      { String targetTag = ys[0].getTag(); 

        for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yy = ys[i]; 

          if (xx == null || yy == null)
          { return false; } 

          Vector xxterms = xx.getTerms();
          Vector yyterms = yy.getTerms(); 
          Vector tterms = new Vector(); 
 
          for (int j = 0; j < xxterms.size(); j++) 
          { ASTTerm xj = (ASTTerm) xxterms.get(j); 
            ASTTerm tj = mod.getCorrespondingTree(xj);
 
            System.out.println(">>^^>> Corresponding tree of " + xj + " is " + tj); 
 
            if (tj != null) 
            { if (tj.getTag().equals(targetTag))
              { tterms.addAll(tj.getTerms()); }
              else 
              { return false; } 
            }  
          }  
            
          System.out.println(">>> Mapped terms = " + tterms); 
          System.out.println(">>> Target terms = " + yyterms); 

          if (tterms.size() == 0) 
          { return false; } 
          else if (AuxMath.isSequencePrefix(tterms,yyterms)) { } 
          else 
          { return false; }

          Vector suff = 
            AuxMath.sequenceSuffix(tterms,yyterms);
          suffixes.add(suff + "");  
        } 

        System.out.println("--> Suffixes are: " + suffixes); 

        if (suffixes.size() == 1)
        { return true; } 

        return false; 
      } 

      return false; 
    }

    public static boolean treesuffixFunction(ASTTerm[] xs, 
                        ASTTerm[] ys, ModelSpecification mod)
    { // Each ys[i].terms == concatenation of xs[i].terms
      // plus a suffix that is a function of some source term. 

      // Assume all source terms have same tag & arity
      // All target terms have same tag

      System.out.println(">&>&> Checking function suffix relation of trees"); 

      
      Vector suffixes = new Vector(); 

      if (ys.length > 1 && xs.length == ys.length)
      { int n = xs[0].arity(); 
        Vector[] xtermvectors = new Vector[n]; 
        String targetTag = ys[0].getTag(); 

        for (int i = 0; i < n; i++)
        { xtermvectors[i] = new Vector(); } 

        for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yy = ys[i]; 

          if (xx == null || yy == null)
          { return false; } 

          Vector xxterms = xx.getTerms();
          Vector yyterms = yy.getTerms(); 
          Vector tterms = new Vector(); 
 
          for (int j = 0; j < xxterms.size(); j++) 
          { ASTTerm xj = (ASTTerm) xxterms.get(j); 
            ASTTerm tj = mod.getCorrespondingTree(xj);
 
            System.out.println(">>^^>> Corresponding tree of " + xj + " is " + tj); 
 
            if (xj instanceof ASTSymbolTerm) 
            { xtermvectors[j].add(xj); } 
            else if (tj != null) 
            { if (tj.getTag().equals(targetTag))
              { tterms.addAll(tj.getTerms()); }
              else 
              { return false; } 
            }  
            else // Not included in concatenation
            { xtermvectors[j].add(xj); } 
          }  
            
          System.out.println(">>> Mapped terms = " + tterms); 
          System.out.println(">>> Target terms = " + yyterms); 

          if (tterms.size() == 0) 
          { return false; } 
          else if (AuxMath.isSequencePrefix(tterms,yyterms)) { } 
          else 
          { return false; }

          Vector suff = 
            AuxMath.sequenceSuffix(tterms,yyterms);
          suffixes.add(suff + "");  
        } 

        System.out.println("--> Suffixes are: " + suffixes); 

        if (suffixes.size() == 1)
        { return true; } 

        // Look for function from some non-empty
        // xtermvectors[j] to suffixes. 

        for (int j = 0; j < xtermvectors.length; j++) 
        { Vector sjterms = xtermvectors[j]; 
          if (sjterms.size() == suffixes.size())
          { if (functionalASTMapping(sjterms,suffixes))
            { System.out.println("--> Functional mapping from " + sjterms + " to " + suffixes); 
              return true; 
            } // But it may not be a valid function. 
          } 
        }  

        return false; 
      } 

      return false; 
    }

    public static boolean treeprefixFunction(ASTTerm[] xs, 
                        ASTTerm[] ys, ModelSpecification mod)
    { // Each ys[i].terms == concatenation prefix + 
      // xs[i].terms
      
      // Assume all source terms have same tag & arity
      // All target terms have same tag

      System.out.println(">&>&> Checking function prefix relation of trees"); 

      
      Vector prefixes = new Vector(); 

      if (ys.length > 1 && xs.length == ys.length)
      { int n = xs[0].arity(); 
        Vector[] xtermvectors = new Vector[n]; 
        String targetTag = ys[0].getTag(); 

        for (int i = 0; i < n; i++)
        { xtermvectors[i] = new Vector(); } 

        for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yy = ys[i]; 

          if (xx == null || yy == null)
          { return false; } 

          Vector xxterms = xx.getTerms();
          Vector yyterms = yy.getTerms(); 
          Vector tterms = new Vector(); 
 
          for (int j = 0; j < xxterms.size(); j++) 
          { ASTTerm xj = (ASTTerm) xxterms.get(j); 
            ASTTerm tj = mod.getCorrespondingTree(xj);
 
            System.out.println(">><<>> Corresponding tree of " + xj + " is " + tj); 
 
            if (xj instanceof ASTSymbolTerm) 
            { xtermvectors[j].add(xj); } 
            else if (tj != null) 
            { if (tj.getTag().equals(targetTag))
              { tterms.addAll(tj.getTerms()); }
              else 
              { return false; } 
            }  
            else 
            { xtermvectors[j].add(xj); } 
          }  
            
          System.out.println(">>> Mapped terms = " + tterms); 
          System.out.println(">>> Target terms = " + yyterms); 

          if (tterms.size() == 0) 
          { return false; } 
          else if (AuxMath.isSequenceSuffix(tterms,yyterms)) { } 
          else 
          { return false; }

          Vector pref = 
            AuxMath.sequencePrefix(tterms,yyterms);
          prefixes.add(pref + "");  
        } 

        System.out.println("--> Prefixes are: " + prefixes); 

        if (prefixes.size() == 1)
        { return true; } 

        // Look for function from some non-empty
        // xtermvectors[j] to suffixes. 

        for (int j = 0; j < xtermvectors.length; j++) 
        { Vector sjterms = xtermvectors[j]; 
          System.out.println("<<> Checking Functional mapping from " + sjterms + " to " + prefixes); 
              
          if (sjterms.size() == prefixes.size())
          { if (functionalASTMapping(sjterms,prefixes))
            { System.out.println("--> Functional mapping from " + sjterms + " to " + prefixes); 
              return true; 
            } 
          } // May not be valid
        }  

        return false; 
      } 

      return false; 
    }

    public static AttributeMatching 
      suffixTreeFunctionMapping(ASTTerm[] xs, 
                        ASTTerm[] ys, ModelSpecification mod,
                        Entity sent, Attribute satt, 
                        Attribute tatt, Vector sourceatts,
                        Vector tms, Vector ams)
    { // Each ys[i].terms == concatenation of xs[i].terms 
      // plus constant suffix of terms. 

      Vector pars = new Vector(); 
      Vector tpars = new Vector(); 

      if (ys.length > 1 && xs.length == ys.length)
      { ASTTerm xx0 = xs[0]; 
        ASTTerm yy0 = ys[0]; 

        String ttag = yy0.getTag(); // All the same. 

        if (xx0 == null || yy0 == null)
        { return null; } 

        int n = xs[0].arity(); // All the same.  
        Vector[] xtermvectors = new Vector[n]; 
        ASTTerm[] targetValues = new ASTTerm[xs.length]; 

        for (int i = 0; i < n; i++)
        { xtermvectors[i] = new Vector(); } 

        Vector suffixes = new Vector();
        Vector suffixtermseqs = new Vector(); 
          // sequences of suffix terms.  

        for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yy = ys[i]; 

          if (xx == null || yy == null)
          { return null; } 

          Vector xxterms = xx.getTerms();
          Vector yyterms = yy.getTerms(); 
          Vector tterms = new Vector(); 
 
          for (int j = 0; j < xxterms.size(); j++) 
          { ASTTerm xj = (ASTTerm) xxterms.get(j); 
            ASTTerm tj = mod.getCorrespondingTree(xj);
 
            if (xj instanceof ASTSymbolTerm) 
            { xtermvectors[j].add(xj); } 
            else if (tj != null) 
            { tterms.addAll(tj.getTerms()); } 
            else 
            { xtermvectors[j].add(xj); } 
          }  
            
            System.out.println(">>> Mapped terms = " + tterms); 
            System.out.println(">>> Target terms = " + yyterms); 

          Vector suff = 
              AuxMath.sequenceSuffix(tterms,yyterms);
          suffixes.add(suff + "");  
          ASTTerm targterm = new ASTCompositeTerm(ttag,suff); 
          suffixtermseqs.add(targterm);
          targetValues[i] = targterm;  
        }
 
        Vector xx0terms = xx0.getTerms();
        Vector yy0terms = yy0.getTerms();
          
        for (int j = 0; j < xx0terms.size(); j++) 
        { ASTTerm xj = (ASTTerm) xx0terms.get(j); 
          ASTTerm tj = mod.getCorrespondingTree(xj);
  
          if (xj instanceof ASTSymbolTerm)
          { if (AuxMath.isConstantSeq(xtermvectors[j]))
            { pars.add(
                new BasicExpression(xj.literalForm()));
            } 
            else
            { pars.add(
                new BasicExpression("_" + (j+1))); 
            }  
          } 
          else if (tj != null) 
          { pars.add(
                new BasicExpression("_" + (j+1))); 
            tpars.add(
                new BasicExpression("_" + (j+1))); 
          }
          else 
          { pars.add(
                new BasicExpression("_" + (j+1)));
          } 
            // else - a source term that the suffix could 
            // functionally depend upon.  
        }  

        boolean found = false; 

        for (int j = 0; j < xtermvectors.length && !found; j++) 
        { Vector sjterms = xtermvectors[j]; 
          if (sjterms.size() == suffixes.size())
          { if (functionalASTMapping(sjterms,suffixes))
            { System.out.println("--> Functional mapping from " + sjterms + " to " + suffixes); 
              java.util.Map sattvalueMap = 
                 new java.util.HashMap(); 

              ASTTerm[] sjtermvect = 
                 new ASTTerm[sjterms.size()];
              for (int p = 0; p < sjterms.size(); p++)
              { sjtermvect[p] = (ASTTerm) sjterms.get(p); }
 
              sattvalueMap.put(satt, sjtermvect); 
              AttributeMatching amsuffix = 
                mod.composedTreeFunction(sent,tatt,
                      sourceatts,
                      sattvalueMap,targetValues,
                      suffixtermseqs, tms, ams); 

              if (amsuffix != null && 
                  amsuffix.trgvalue != null) 
              { BasicExpression nameexpr = 
                  new BasicExpression("name"); 
                nameexpr.setUmlKind(Expression.FUNCTION);
                nameexpr.addParameter(
                  new BasicExpression("_" + (j+1)));  
                Expression newjpar = 
                  amsuffix.trgvalue.substituteEq(
                                         "_1",nameexpr);  
                tpars.add(newjpar); 
                found = true;
              }  
              else 
              { System.out.println("!! No mapping from " + sjterms + " to " + suffixes); 
              } 
            } 
          } 
        }  

        if (!found) 
        { return null; } 
                  
        BasicExpression srcterm = 
          new BasicExpression(xs[0]); 
        srcterm.setParameters(pars); 

        BasicExpression trgterm = 
          new BasicExpression(ys[0]); 
        trgterm.setParameters(tpars); 

        return new AttributeMatching(srcterm,trgterm); 
      }
 
      return null; 
    }

    public static AttributeMatching 
      prefixTreeFunctionMapping(ASTTerm[] xs, 
                        ASTTerm[] ys, ModelSpecification mod,
                        Entity sent, Attribute satt, 
                        Attribute tatt, Vector sourceatts,
                        Vector tms, Vector ams)
    { // Each ys[i].terms == concatenation of prefix     
      // plus xs[i].terms 
      
      Vector pars = new Vector(); 
      Vector tpars = new Vector(); 

      if (ys.length > 1 && xs.length == ys.length)
      { ASTTerm xx0 = xs[0]; 
        ASTTerm yy0 = ys[0]; 

        String ttag = yy0.getTag(); // All the same. 

        if (xx0 == null || yy0 == null)
        { return null; } 


        int n = xs[0].arity(); 
        Vector[] xtermvectors = new Vector[n]; 
        ASTTerm[] targetValues = new ASTTerm[xs.length]; 

        for (int i = 0; i < n; i++)
        { xtermvectors[i] = new Vector(); } 

        Vector prefixes = new Vector();
        Vector prefixtermseqs = new Vector(); 
          // sequences of prefix terms.  

        for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yy = ys[i]; 

          if (xx == null || yy == null)
          { return null; } 

          Vector xxterms = xx.getTerms();
          Vector yyterms = yy.getTerms(); 
          Vector tterms = new Vector(); 
 
          for (int j = 0; j < xxterms.size(); j++) 
          { ASTTerm xj = (ASTTerm) xxterms.get(j); 
            ASTTerm tj = mod.getCorrespondingTree(xj);
 
            if (xj instanceof ASTSymbolTerm) 
            { xtermvectors[j].add(xj); } 
            else if (tj != null) 
            { tterms.addAll(tj.getTerms()); } 
            else 
            { xtermvectors[j].add(xj); } 
          }  
            
            System.out.println(">>> Mapped terms = " + tterms); 
            System.out.println(">>> Target terms = " + yyterms); 

          Vector pref = 
              AuxMath.sequencePrefix(tterms,yyterms);
          prefixes.add(pref + "");  
          ASTTerm targterm = new ASTCompositeTerm(ttag,pref); 
          prefixtermseqs.add(targterm);
          targetValues[i] = targterm;  
        }

        Vector xx0terms = xx0.getTerms();
        Vector yy0terms = yy0.getTerms();
          
        for (int j = 0; j < xx0terms.size(); j++) 
        { ASTTerm xj = (ASTTerm) xx0terms.get(j); 
          ASTTerm tj = mod.getCorrespondingTree(xj);
  
          if (xj instanceof ASTSymbolTerm)
          { if (AuxMath.isConstantSeq(xtermvectors[j]))
            { pars.add(
                new BasicExpression(xj.literalForm())); 
            } 
            else 
            { pars.add(
                new BasicExpression("_" + (j+1))); 
            } 
          } 
          else if (tj != null) 
          { pars.add(
                new BasicExpression("_" + (j+1))); 
            tpars.add(
                new BasicExpression("_" + (j+1))); 
          }
          else 
          { pars.add(
                new BasicExpression("_" + (j+1)));
          } 
            // else - a source term that the suffix could 
            // functionally depend upon.  
        }  
 
        boolean found = false; 
        for (int j = 0; j < xtermvectors.length && !found; j++) 
        { Vector sjterms = xtermvectors[j]; 
          System.out.println("<<>> Checking functional mapping from " + sjterms + " to " + prefixes); 
            
          if (sjterms.size() == prefixes.size())
          { System.out.println("<<>> Checking functional mapping from " + sjterms + " to " + prefixes); 
              
            if (functionalASTMapping(sjterms,prefixes))
            { System.out.println("<<>> Functional mapping from " + sjterms + " to " + prefixes); 
              java.util.Map sattvalueMap = 
                 new java.util.HashMap(); 

              ASTTerm[] sjtermvect = 
                 new ASTTerm[sjterms.size()];
              for (int p = 0; p < sjterms.size(); p++)
              { sjtermvect[p] = (ASTTerm) sjterms.get(p); }
 
              sattvalueMap.put(satt, sjtermvect); 
              AttributeMatching amprefix = 
                mod.composedTreeFunction(sent,tatt,
                      sourceatts,
                      sattvalueMap,targetValues,
                      prefixtermseqs, tms, ams); 

              if (amprefix != null && 
                  amprefix.trgvalue != null) 
              { BasicExpression nameexpr = 
                  new BasicExpression("name"); 
                nameexpr.setUmlKind(Expression.FUNCTION);
                nameexpr.addParameter(
                  new BasicExpression("_" + (j+1)));  
                Expression newjpar = 
                  amprefix.trgvalue.substituteEq(
                                         "_1",nameexpr);  
                tpars.add(0,newjpar); 
                found = true;
              }  
              
            } 
          } 
        }  
              
        if (!found) 
        { return null; } 
    
        BasicExpression srcterm = 
          new BasicExpression(xs[0]); 
        srcterm.setParameters(pars); 

        BasicExpression trgterm = 
          new BasicExpression(ys[0]); 
        trgterm.setParameters(tpars); 

        return new AttributeMatching(srcterm,trgterm); 
      }
 
      return null; 
    }

    public static AttributeMatching 
      suffixTreeMapping(ASTTerm[] xs, 
                        ASTTerm[] ys, ModelSpecification mod,
                        Vector tms)
    { // Each ys[i].terms == concatenation of xs[i].terms 
      // plus suffix of terms, function of some x term. 

      Vector pars = new Vector(); 
      Vector tpars = new Vector(); 
      Vector tterms = new Vector(); 

      if (ys.length > 1 && xs.length == ys.length)
      { // for (int i = 0; i < xs.length; i++)
        // { 
          ASTTerm xx = xs[0]; 
          ASTTerm yy = ys[0]; 

          if (xx == null || yy == null)
          { return null; } 

          Vector xxterms = xx.getTerms();
          Vector yyterms = yy.getTerms();
          
          for (int j = 0; j < xxterms.size(); j++) 
          { ASTTerm xj = (ASTTerm) xxterms.get(j); 
            ASTTerm tj = mod.getCorrespondingTree(xj);
  
            if (xj instanceof ASTSymbolTerm)
            { pars.add(
                new BasicExpression(xj.literalForm())); 
            } 
            else if (tj != null) 
            { pars.add(
                new BasicExpression("_" + (j+1))); 
              tpars.add(
                new BasicExpression("_" + (j+1))); 
              tterms.addAll(tj.getTerms());  
            }
            else 
            { pars.add(
                new BasicExpression("_" + (j+1))); 
            } 

            // else - a source term that the suffix could 
            // functionally depend upon. 
          }  

          for (int j = tterms.size(); j < yyterms.size(); j++)
          { ASTTerm yj = (ASTTerm) yyterms.get(j);
            tpars.add(new BasicExpression(yj)); 
          } // suffix 
            
        // }     
          
        BasicExpression srcterm = 
          new BasicExpression(xs[0]); 
        srcterm.setParameters(pars); 

        BasicExpression trgterm = 
          new BasicExpression(ys[0]); 
        trgterm.setParameters(tpars); 

        return new AttributeMatching(srcterm,trgterm); 
      } 
      return null; 
    }

    public static boolean tree2sequenceMapping(ASTTerm[] xs, 
                        ASTTerm[] ys, ModelSpecification mod)
    { // Each ys[i].terms == same composition of mapped
      // xs[i].terms
      // plus constant prefixes/inserts/suffixes of terms. 

      System.out.println(">&*>&*> Checking tree to sequence mapping"); 


      java.util.HashSet patterns = new java.util.HashSet(); 
      java.util.HashSet unuseds = new java.util.HashSet(); 


      if (ys.length > 1 && xs.length == ys.length)
      { String targettag = ys[0].getTag(); 


        for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yy = ys[i]; 

          if (xx == null || yy == null)
          { return false; } 

          Vector xxterms = xx.getTerms();
          Vector targets = yy.getTerms();

          Vector sources = new Vector(); 

          for (int j = 0; j < xxterms.size(); j++) 
          { ASTTerm xj = (ASTTerm) xxterms.get(j); 
            ASTTerm tj = mod.getCorrespondingTree(xj);
  
            if (xj instanceof ASTSymbolTerm)
            { } 
            else if (tj != null) 
            { // tj tag should be targettag

              if (tj.getTag().equals(targettag))
              { sources.add(tj.getTerms()); } 
              else 
              { return false; } 
            } 
            else 
            { return false; } 
          }

          Vector unused = new Vector(); 
          Vector pattern = 
            AuxMath.sequenceComposition(
              sources,targets,unused); 
  
          patterns.add(pattern + "");
          unuseds.add(unused + "");  
        } 
      } 

      System.out.println("Unused source terms: " + unuseds); 
      System.out.println("Mapping patterns: " + patterns); 

      if (patterns.size() == 1 && unuseds.size() == 1)
      { return true; }
      return false; 
    } 
 
    public static AttributeMatching 
      tree2SequenceMap(ASTTerm[] xs, 
                        ASTTerm[] ys, ModelSpecification mod,
                        Entity sent, Attribute satt, 
                        Attribute tatt, Vector sourceatts,
                        Vector tms, Vector ams)
    { // Each ys[i].terms == concatenation of xs[i].terms 
      // plus constant suffix of terms. 

      System.out.println(">&*>&*> Assembling tree to sequence mapping"); 

      java.util.HashSet patterns = new java.util.HashSet(); 
      java.util.HashSet unuseds = new java.util.HashSet(); 

      Vector patternSequences = new Vector(); 

      if (ys.length > 1 && xs.length == ys.length)
      { String targettag = ys[0].getTag(); 

        for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yy = ys[i]; 

          if (xx == null || yy == null)
          { return null; } 

          Vector xxterms = xx.getTerms();
          Vector targets = yy.getTerms();

          Vector sources = new Vector(); 

          for (int j = 0; j < xxterms.size(); j++) 
          { ASTTerm xj = (ASTTerm) xxterms.get(j); 
            ASTTerm tj = mod.getCorrespondingTree(xj);
  
            if (xj instanceof ASTSymbolTerm)
            { } 
            else if (tj != null) 
            { sources.add(tj.getTerms()); } 
            else 
            { return null; } 
          }

          Vector unused = new Vector(); 
          Vector pattern = 
            AuxMath.sequenceComposition(
              sources,targets,unused); 
  
          patterns.add(pattern + "");
          patternSequences.add(pattern); 
          unuseds.add(unused + "");  
        } 
     
        System.out.println("Unused source terms: " + unuseds); 
        System.out.println("Mapping patterns: " + patterns); 

        if (patterns.size() != 1 || unuseds.size() != 1)
        { return null; }
 
        Vector pars = new Vector(); 
        Vector tpars = new Vector(); 

        ASTTerm xx0 = xs[0]; 

        Vector xx0terms = xx0.getTerms();
        int xtermcount = 0; 
    
        for (int j = 0; j < xx0terms.size(); j++) 
        { ASTTerm xj = (ASTTerm) xx0terms.get(j); 
          ASTTerm tj = mod.getCorrespondingTree(xj);
        
          if (xj instanceof ASTSymbolTerm)
          { pars.add(
                new BasicExpression(xj.literalForm())); 
          } 
          else if (tj != null) 
          { pars.add(
                new BasicExpression("_" + (xtermcount+1))); 
          // tpars.add(
          //       new BasicExpression("_" + (xtermcount+1))); 
            xtermcount++; 
          }
        }
      
      // int n = xs[0].arity(); 
      // Vector[] xtermvectors = new Vector[n]; 
      // ASTTerm[] targetValues = new ASTTerm[xs.length]; 

      // for (int i = 0; i < n; i++)
      // { xtermvectors[i] = new Vector(); } 

      //  Vector suffixes = new Vector();
      //  Vector suffixtermseqs = new Vector(); 
          // sequences of suffix terms.  

        if (patternSequences.size() == 0) 
        { return null; } 

        Vector pseq = (Vector) patternSequences.get(0); 

        for (int k = 0; k < pseq.size(); k++) 
        { Object tt = pseq.get(k);

          if (tt instanceof ASTTerm) 
          { tpars.add(
               new BasicExpression((ASTTerm) tt));
          } 
          else if (tt instanceof String)
          { tpars.add(new BasicExpression((String) tt)); } 
        }
                     
        BasicExpression srcterm = 
          new BasicExpression(xs[0]); 
        srcterm.setParameters(pars); 

        BasicExpression trgterm = 
          new BasicExpression(ys[0]); 
        trgterm.setParameters(tpars); 

        return new AttributeMatching(srcterm,trgterm); 
      }
 
      return null; 
    }

    public static Vector targetBrackets(ASTTerm[] xs, ASTTerm[] ys)
    { // Is each ys[i].terms starts with same symbol and 
      // ends with another or same symbol? Return these symbols 
      Vector res = new Vector(); 
      String startSymbol = null; 
      String endSymbol = null; 
      
      if (ys.length > 1 && xs.length == ys.length)
      { for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yy = ys[i]; 

          if (xx == null || yy == null)
          { return null; } 

          int n = xx.arity(); 
          int m = yy.arity(); 
        
          if (m < 2) 
          { return null; } 

          // ASTTerm s0 = xx.getTerm(0); 
          // ASTTerm sn = xx.getTerm(n-1); 

          ASTTerm t0 = yy.getTerm(0); 
          ASTTerm tn = yy.getTerm(m-1); 

          if (t0 instanceof ASTSymbolTerm && 
              tn instanceof ASTSymbolTerm)
          { if (startSymbol == null) 
            { startSymbol = t0.literalForm(); } 
            else if (startSymbol.equals(t0.literalForm())) { } 
            else 
            { return null; } // not consistent start symbols

            if (endSymbol == null) 
            { endSymbol = tn.literalForm(); } 
            else if (endSymbol.equals(tn.literalForm())) { } 
            else 
            { return null; } // not consistent end symbols
          } 
          else 
          { return null; } 
          // Should also be different to s0, sn
        } 
        
        if (startSymbol != null && endSymbol != null)
        { res.add(startSymbol); 
          res.add(endSymbol); 
          return res; 
        } 
      } 
      return null; 
    }


/* 
    public static boolean embeddedTrees(ASTTerm[] xs, ASTTerm[] ys, ModelSpecification mod)
    { // Is each ys[i] = (tag xs[i]' ..terms..) for same tag? 

      if (ys.length > 1 && xs.length == ys.length)
      { for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yvect = ys[i]; 

          System.out.println(">>>> Comparing " + xx + " to " + yvect); 

          if (yvect instanceof ASTCompositeTerm && 
              ((ASTCompositeTerm) yvect).getTerms().size() > 1)
          { ASTCompositeTerm ct = (ASTCompositeTerm) yvect; 
            ASTTerm ct0 = (ASTTerm) ct.getTerms().get(0); 
            if (xx.equals(ct0) || 
                mod.correspondingTrees(xx,ct0)) { } 
            else 
            { return false; } 
          } 
          else 
          { return false; } 
        }
        return true; 
      } 
      return false; 
    } */ 

    public static boolean embeddedTrees(Entity sent, ASTTerm[] xs, ASTTerm[] ys, ModelSpecification mod)
    { // Is each ys[i] = (tag xs[i]' ..terms..) for same tag? 

      if (ys.length > 1 && xs.length == ys.length)
      { for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yvect = ys[i]; 

          System.out.println(">>>> Comparing " + xx + " to " + yvect); 

          if (yvect instanceof ASTCompositeTerm && 
              ((ASTCompositeTerm) yvect).getTerms().size() > 1)
          { ASTCompositeTerm ct = (ASTCompositeTerm) yvect; 
            ASTTerm ct0 = (ASTTerm) ct.getTerms().get(0); 
            if (xx.equals(ct0) || 
                mod.correspondingTrees(sent, xx, ct0)) { } 
            else 
            { return false; } 
          } 
          else 
          { return false; } 
        }
        return true; 
      } 
      return false; 
    }

 /*
  public static boolean nestedSingletonTrees(ASTTerm[] xs, ASTTerm[] ys, ModelSpecification mod)
    { // Is each xs[i] = (tag1 pi) where ys[i] = (tag2 pi) 

      if (ys.length > 1 && xs.length == ys.length)
      { for (int i = 0; i < xs.length; i++)
        { ASTTerm xx = xs[i]; 
          ASTTerm yvect = ys[i]; 

          System.out.println(">>>> Comparing " + xx + " to " + yvect); 

          if (yvect instanceof ASTBasicTerm && 
              xx instanceof ASTBasicTerm) 
          { String yy = ((ASTBasicTerm) yvect).getValue();
            String xsym = ((ASTBasicTerm) xx).getValue();  
            if (yy.equals(xsym)) { }
            else 
            { return false; }
          } 
          else if (yvect instanceof ASTCompositeTerm && 
                   ((ASTCompositeTerm) yvect).getTerms().size() == 1 && 
                   xx instanceof ASTCompositeTerm && 
                   ((ASTCompositeTerm) xx).getTerms().size() == 1)
          { ASTCompositeTerm ct = (ASTCompositeTerm) yvect; 
            ASTTerm ct0 = (ASTTerm) ct.getTerms().get(0); 
            ASTCompositeTerm xt = (ASTCompositeTerm) xx; 
            ASTTerm xt0 = (ASTTerm) xt.getTerms().get(0); 
            if (xt0.equals(ct0) || 
                mod.correspondingTrees(xt0,ct0)) { } 
            else 
            { return false; } 
          } 
          else 
          { return false; } 
        }
        return true; 
      } 
      return false; 
    } */ 

  public static boolean nestedSingletonTrees(Entity sent, ASTTerm[] xs, ASTTerm[] ys, ModelSpecification mod)
  { // Is each xs[i] = (tag1 pi) where ys[i] = (tag2 pi) 
    // Or (tag1 pi) (tag2 qi) where the pi ~ qi

    if (ys.length > 1 && xs.length == ys.length)
    { for (int i = 0; i < xs.length; i++)
      { ASTTerm xx = xs[i]; 
        ASTTerm yvect = ys[i]; 

        System.out.println(">>>> Comparing " + xx + " to " + yvect); 

        if (yvect instanceof ASTBasicTerm && 
            xx instanceof ASTBasicTerm) 
        { String yy = ((ASTBasicTerm) yvect).getValue();
          String xsym = ((ASTBasicTerm) xx).getValue();  
          if (yy.equals(xsym)) { }
          else 
          { return false; }
        } 
        else if (yvect instanceof ASTCompositeTerm && 
                 ((ASTCompositeTerm) yvect).getTerms().size() == 1 && 
                 xx instanceof ASTCompositeTerm && 
                 ((ASTCompositeTerm) xx).getTerms().size() == 1)
        { ASTCompositeTerm ct = (ASTCompositeTerm) yvect; 
          ASTTerm ct0 = (ASTTerm) ct.getTerms().get(0); 
          ASTCompositeTerm xt = (ASTCompositeTerm) xx; 
          ASTTerm xt0 = (ASTTerm) xt.getTerms().get(0); 
          if (xt0.equals(ct0) || 
              mod.correspondingTrees(sent,xt0,ct0)) 
          { System.out.println(">> Corresponding terms: " + xt0 + " " + ct0); } 
          else 
          { return false; } 
        } 
        else 
        { return false; } 
      }
      return true; 
    } 
    return false; 
  }

  public static AttributeMatching 
    compositeSource2TargetTrees(
      Entity sent, ASTTerm[] xs, ASTTerm[] ys, 
      ModelSpecification mod)
  { // Is there an index j of each xs[i] = (tag1 p1 ... pn) 
    // each pj = ys[i]  
    // or pj ~ ys[i]?
    // The nested mapping is then  _j |-->_1
    // 
    // result = null indicates failure. 

    AttributeMatching res = null; 

    if (ys.length > 1 && xs.length == ys.length)
    { ASTTerm s0 = xs[0]; 
      int xarity = s0.arity(); 

      for (int j = 0; j < xarity; j++) 
      { Vector jvect = new Vector(); 
        ASTTerm[] jterms = 
          ASTTerm.subterms(xs,j,jvect); 
        boolean jmatch = true; 
    
        for (int i = 0; i < xs.length && jmatch; i++)
        { ASTTerm xx = jterms[i]; 
          ASTTerm yy = ys[i]; 

          System.out.println(">>>> Comparing " + (j+1) + "th source subterm " + xx + " to " + yy); 

          if (xx.equals(yy) || 
              mod.correspondingTrees(xx,yy)) 
          { System.out.println(">>-- Corresponding terms: " + xx + " " + yy); } 
          else 
          { jmatch = false; } 
        }

        if (jmatch) 
        { System.out.println(">> match from " + (j+1) + 
                             " source subterms to target"); 
          BasicExpression sexpr = 
            BasicExpression.newASTBasicExpression(s0);
          
          ASTTerm targ0 = ys[0]; 
          BasicExpression texpr = new BasicExpression(targ0);
          Vector newpars = new Vector(); 
          newpars.add(new BasicExpression("_" + (j+1))); 
          texpr.setParameters(newpars); 
 
          AttributeMatching amx = 
            new AttributeMatching(sexpr, texpr);
          return amx;   
        } 
      } // try next j
    } 
    return null; 
  }


  public static ASTTerm[] composeIntoTrees(Vector termSeqs, Vector res)
  { int n = termSeqs.size(); 
    if (n == 0) 
    { return null; } 
    ASTTerm[] terms0 = (ASTTerm[]) termSeqs.get(0); 
    ASTTerm[] trees = new ASTTerm[terms0.length]; 

    for (int i = 0; i < terms0.length; i++) 
    { ASTCompositeTerm ct = 
        new ASTCompositeTerm("TAG"); 
      for (int j = 0; j < n; j++) 
      { ASTTerm[] termsj = (ASTTerm[]) termSeqs.get(j); 
        ct.addTerm(termsj[i]); 
      } 
      trees[i] = ct; 
      res.add(ct); 
    } 
    return trees; 
  } 

  public static Vector generateASTExamples(Vector javaASTs)
  { /* Generate corresponding OCL and Java example ASTs */
    Vector res = new Vector(); 

    /* Basic numeric expressions: */ 

    for (int i = 500; i < 700; i++)
    { ASTBasicTerm tocl = new ASTBasicTerm("BasicExpression", ""+i); 
      res.add(tocl); 
      ASTBasicTerm javat1 = new ASTBasicTerm("integerLiteral", ""+i); 
      ASTCompositeTerm javat2 = new ASTCompositeTerm("literal", javat1);
      ASTCompositeTerm javat3 = new ASTCompositeTerm("expression", javat2);
      javaASTs.add(javat3); 
    }

    /* Unary expressions: */ 

    Vector oclunary = new Vector(); 
    Vector junary = new Vector();
 
    for (int i = 0; i < 200; i++)
    { Vector args = new Vector(); 
      args.add(new ASTSymbolTerm("-")); 
      args.add(res.get(i)); 
      ASTCompositeTerm ounary = 
        new ASTCompositeTerm("UnaryExpression", args); 
      oclunary.add(ounary); 
     
      Vector jargs = new Vector(); 
      jargs.add(new ASTSymbolTerm("-")); 
      jargs.add(javaASTs.get(i)); 
      ASTCompositeTerm jx = 
        new ASTCompositeTerm("expression", jargs); 
      junary.add(jx); 
    } 

    res.addAll(oclunary); 
    javaASTs.addAll(junary); 
      

    return res;  

  /* (expression (primary (literal (floatLiteral 0.33)))) */ 
  /* (expression (primary (literal "double"))) */ 
  /* (expression (primary (literal true))) */ 
  /* (expression (primary id)) */ 


    /* Binary numeric expressions: 

    ASTBasicTerm t1 = new ASTBasicTerm("BasicExpression", "1"); 
    ASTSymbolTerm st = new ASTSymbolTerm("+"); 
    ASTBasicTerm t2 = new ASTBasicTerm("BasicExpression", "2"); 
    Vector args = new Vector(); 
    args.add(t1); args.add(st); args.add(t2); 
    ASTCompositeTerm t = new ASTCompositeTerm("BinaryExpression", args); 
    res.add(t); 

    ASTBasicTerm jt1 = new ASTBasicTerm("integerLiteral", "1"); 
    ASTCompositeTerm jt2 = new ASTCompositeTerm("literal", jt1);
    ASTCompositeTerm jt3 = new ASTCompositeTerm("expression", jt2);
    ASTBasicTerm jt4 = new ASTBasicTerm("integerLiteral", "2"); 
    ASTCompositeTerm jt5 = new ASTCompositeTerm("literal", jt4);
    ASTCompositeTerm jt6 = new ASTCompositeTerm("expression", jt5); 
    ASTSymbolTerm stj = new ASTSymbolTerm("+"); 
    Vector jargs = new Vector(); 
    jargs.add(jt3); jargs.add(stj); jargs.add(jt6);
    ASTCompositeTerm tj = 
      new ASTCompositeTerm("expression", jargs); 
    javaASTs.add(tj); 
    return res; */ 
  }  

  public static void entitiesFromASTs(Vector asts, String suff, Vector es)
  { // For each tag t, create entity t+suff. Add subclass for 
    // each different arity of t terms.
    // Nesting: (t (t1 x) (t2 y)) becomes
    // t_t1$t_t2, etc.  

    Type treeType = new Type("OclAny", null); 

    for (int i = 0; i < asts.size(); i++) 
    { ASTTerm t = (ASTTerm) asts.get(i); 
      String tg = t.getTag() + suff; 
      int n = t.arity(); 
      if (n > 0) 
      { Entity ee = (Entity) ModelElement.lookupByName(tg,es); 
        if (ee == null) 
        { ee = new Entity(tg);

          System.out.println("Created entity: "  + tg); 
 
          if (suff.equals(""))
          { ee.addStereotype("source"); } 
          else 
          { ee.addStereotype("target"); } 
           
          ee.setAbstract(true); 
          Attribute astatt = 
            new Attribute("ast", treeType, ModelElement.INTERNAL); 
          ee.addAttribute(astatt); 
          es.add(ee); 
        } 

        String enname = tg + "_" + n; 
        Entity en = 
          (Entity) ModelElement.lookupByName(enname,es);
        if (en == null) 
        { en = new Entity(enname); 

          if (suff.equals(""))
          { en.addStereotype("source"); } 
          else 
          { en.addStereotype("target"); } 

          en.setSuperclass(ee); 
          ee.addSubclass(en); 
          System.out.println("Created entity: "  + enname); 
          es.add(en); 
        } 
      } 
    } 
  } 

  public static void deepEntitiesFromASTs(Vector asts, String suff, Vector es)
  { // For each different term t, create entity t' + suff.
    // Nesting: (t (t1 x) (t2 y)) entity has name
    // t_t1$t_t2, etc.  

    Type treeType = new Type("OclAny", null); 

    for (int i = 0; i < asts.size(); i++) 
    { ASTTerm t = (ASTTerm) asts.get(i); 
      String tg = t.tagFunction() + suff; 
      String tgsup = t.getTag() + suff; 
      int n = t.arity(); 
      if (n > 0) 
      { Entity sup = (Entity) ModelElement.lookupByName(tgsup,es); 
        if (sup == null) 
        { sup = new Entity(tgsup);

          System.out.println("Created entity: "  + sup); 
 
          if (suff.equals(""))
          { sup.addStereotype("source"); } 
          else 
          { sup.addStereotype("target"); } 
           
          sup.setAbstract(true); 
          Attribute astatt = 
            new Attribute("ast", treeType, ModelElement.INTERNAL); 
          sup.addAttribute(astatt); 
          es.add(sup); 
        } 

        Entity ee = (Entity) ModelElement.lookupByName(tg,es); 
        if (ee == null) 
        { ee = new Entity(tg);

          System.out.println("Created entity: "  + tg); 
 
          if (suff.equals(""))
          { ee.addStereotype("source"); } 
          else 
          { ee.addStereotype("target"); } 
           
          ee.setSuperclass(sup); 
          sup.addSubclass(ee); 
          es.add(ee); 
        } 
      } 
    } 
  } 

  public static Vector entityMatchingsFromASTs(Vector sasts, 
                         Vector tasts, Vector es)
  { // For corresponding s, t create entity matching
    // s.tag_s.arity |--> t.tag$T_t.arity. 

    Vector ems = new Vector(); 

    for (int i = 0; i < sasts.size() && i < tasts.size(); i++) 
    { ASTTerm s = (ASTTerm) sasts.get(i); 
      ASTTerm t = (ASTTerm) tasts.get(i); 
      
      int n = s.arity(); 
      int m = t.arity(); 

      if (n > 0 && m > 0) 
      { String sentname = s.getTag() + "_" + n; 
        Entity sent = 
          (Entity) ModelElement.lookupByName(sentname,es); 
        String tentname = t.getTag() + "$T_" + m; 
        Entity tent = 
          (Entity) ModelElement.lookupByName(tentname,es); 
        if (sent != null && tent != null) 
        { EntityMatching em = 
            new EntityMatching(sent,tent); 
          if (ems.contains(em)) { }
          else 
          { ems.add(em); }  
        } 
      } 
    } 
    return ems; 
  } 

  public static Vector deepEntityMatchingsFromASTs(Vector sasts, 
                         Vector tasts, Vector es)
  { // For corresponding s, t create entity matching
    // s.tag_s.arity |--> t.tag$T_t.arity. 

    Vector ems = new Vector(); 

    for (int i = 0; i < sasts.size() && i < tasts.size(); i++) 
    { ASTTerm s = (ASTTerm) sasts.get(i); 
      ASTTerm t = (ASTTerm) tasts.get(i); 
      
      int n = s.arity(); 
      int m = t.arity(); 

      if (n > 0 && m > 0) 
      { String sentname = s.tagFunction(); // Assume it is deep 
        Entity sent = 
          (Entity) ModelElement.lookupByName(sentname,es); 
        String tentname = t.getTag() + "$T_" + m; 
        Entity tent = 
          (Entity) ModelElement.lookupByName(tentname,es); 
        if (sent != null && tent != null) 
        { EntityMatching em = 
            new EntityMatching(sent,tent); 
          if (ems.contains(em)) { }
          else 
          { ems.add(em); }  
        } 
      } 
    } 
    return ems; 
  } 

  public static void modelSpecificationFromASTs(Vector sasts, 
                         Vector tasts, Vector es, 
                         ModelSpecification mod)
  { // For corresponding s, t create instances
    // s_inst : s.tag_s.arity 
    // s_inst.ast = s
    // t_inst : t.tag$T_t.arity
    // t_inst.ast = t
    // s_inst |-> t_inst 

    for (int i = 0; i < sasts.size() && i < tasts.size(); i++) 
    { ASTTerm s = (ASTTerm) sasts.get(i); 
      ASTTerm t = (ASTTerm) tasts.get(i); 
      
      int n = s.arity(); 
      int m = t.arity(); 

      if (n > 0 && m > 0) 
      { String sentname = s.getTag() + "_" + n; 
        String tentname = t.getTag() + "$T_" + m; 
        String sinst = sentname.toLowerCase() + "_" + i; 
        String tinst = tentname.toLowerCase() + "_" + i; 
        Entity sent = 
          (Entity) ModelElement.lookupByName(sentname,es); 
        ObjectSpecification sobj = 
          new ObjectSpecification(sinst,sentname); 
        sobj.setEntity(sent);
        sobj.setValue("ast", s); 
        Entity tent = 
          (Entity) ModelElement.lookupByName(tentname,es); 
        ObjectSpecification tobj = 
          new ObjectSpecification(tinst,tentname); 
        tobj.setEntity(tent);
        tobj.setValue("ast", t); 
        mod.addObject(sobj); 
        mod.addObject(tobj); 
        mod.addCorrespondence(sobj,tobj); 
      } 
    } 
  } 

  public static void deepModelSpecificationFromASTs(Vector sasts, 
                         Vector tasts, Vector es, 
                         ModelSpecification mod)
  { // For corresponding s, t create instances
    // s_inst : s.tag_s.arity 
    // s_inst.ast = s
    // t_inst : t.tag$T_t.arity
    // t_inst.ast = t
    // s_inst |-> t_inst 

    for (int i = 0; i < sasts.size() && i < tasts.size(); i++) 
    { ASTTerm s = (ASTTerm) sasts.get(i); 
      ASTTerm t = (ASTTerm) tasts.get(i); 
      
      int n = s.arity(); 
      int m = t.arity(); 

      if (n > 0 && m > 0) 
      { String sentname = s.tagFunction(); 
        String tentname = t.getTag() + "$T_" + m; 
        String sinst = sentname.toLowerCase() + "_" + i; 
        String tinst = tentname.toLowerCase() + "_" + i; 
        Entity sent = 
          (Entity) ModelElement.lookupByName(sentname,es); 
        ObjectSpecification sobj = 
          new ObjectSpecification(sinst,sentname); 
        sobj.setEntity(sent);
        sobj.setValue("ast", s); 
        Entity tent = 
          (Entity) ModelElement.lookupByName(tentname,es); 
        ObjectSpecification tobj = 
          new ObjectSpecification(tinst,tentname); 
        tobj.setEntity(tent);
        tobj.setValue("ast", t); 
        mod.addObject(sobj); 
        mod.addObject(tobj); 
        mod.addCorrespondence(sobj,tobj); 
      } 
    } 
  } 

  public static Vector randomBasicASTTermsForTag(String tag, 
                                       int n, int numb)
  { // numb (tag t1 ... tn) terms 
    Vector res = new Vector(); 
    for (int i = 0; i < numb; i++) 
    { Vector args = new Vector(); 
      for (int j = 0; j < n; j++) 
      { String rstring = ModelElement.randomString(10); 
        args.add(new ASTSymbolTerm(rstring)); 
      } 
      ASTCompositeTerm ct = 
        new ASTCompositeTerm(tag,args); 
      res.add(ct); 
    } 
    return res; 
  }    

  public static Vector randomCompositeASTTermsForTag(
    String tag, Vector subtermVectors, 
    int n, int numb)
  { // numb (tag t1 ... tn) terms where ti is a random
    // term from subtermVectors[i-1]
 
    Vector res = new Vector(); 
    for (int i = 0; i < numb; i++) 
    { Vector args = new Vector(); 
      for (int j = 0; j < n; j++) 
      { Vector termsj = (Vector) subtermVectors.get(j); 
        if (termsj.size() == 1) 
        { args.add(termsj.get(0)); } 
        else 
        { args.add(ModelElement.randomElement(termsj)); } 
      } 
      ASTCompositeTerm ct = 
        new ASTCompositeTerm(tag,args); 
      res.add(ct); 
    } 
    return res; 
  }    
    
 
  public static void main(String[] args) 
  { // ASTBasicTerm t = new ASTBasicTerm("OclBasicExpression", "true"); 
    // System.out.println(t.isInteger()); 
    // System.out.println(t.isBoolean());

    ASTBasicTerm tt1 = new ASTBasicTerm("t1", "aa"); 
    ASTBasicTerm tt2 = new ASTBasicTerm("t2", "bb");
    ASTSymbolTerm tts = new ASTSymbolTerm("&"); 
 

    Vector vect = new Vector(); 
    vect.add(tt1); 
    vect.add(tts); 
    vect.add(tt2); 
    
    ASTCompositeTerm ttc = 
       new ASTCompositeTerm("ct", vect); 

    System.out.println(ttc.tagFunction()); 
  }
} 

  /* 
    Vector consts = 
      randomBasicASTTermsForTag("Const", 1, 50);
    Vector ops = new Vector(); 
    ops.add(new ASTSymbolTerm("+"));  
    ops.add(new ASTSymbolTerm("-"));  
    Vector vars = 
      randomBasicASTTermsForTag("Var", 1, 50);
    Vector subs = new Vector(); 
    subs.add(consts); 
    subs.add(ops); 
    subs.add(vars); 


    Vector exprs = 
      randomCompositeASTTermsForTag("Expr", subs, 3, 30); 
      // size 3

    Vector subsx = new Vector(); 
      subsx.add(exprs); 
      subsx.add(ops); 
      subsx.add(exprs); 

    Vector nestedexprs = 
      randomCompositeASTTermsForTag("Expr", subsx, 3, 30);
      // size 7

    Vector compars = new Vector(); 
    compars.add(new ASTSymbolTerm("<")); 
    compars.add(new ASTSymbolTerm(">")); 
    compars.add(new ASTSymbolTerm("==")); 


    Vector subs1 = new Vector(); 
    subs1.add(exprs); 
    subs1.add(compars); 
    subs1.add(exprs); 

    Vector cmps = 
      randomCompositeASTTermsForTag("Cmp", subs1, 3, 30);
  
    System.out.println(cmps); // each has size 7

    Vector assgn = new Vector(); 
    assgn.add(new ASTSymbolTerm("=")); 

    Vector subs2 = new Vector(); 
    subs2.add(vars); 
    subs2.add(assgn); 
    subs2.add(exprs); 

    Vector assgns = 
      randomCompositeASTTermsForTag("Assign", subs2, 3, 30);

    // System.out.println(assgns); 
    // (Assign v = e) with size 5

    Vector subs2x = new Vector(); 
    subs2x.add(vars); 
    subs2x.add(assgn); 
    subs2x.add(nestedexprs); 

    Vector assgns2 = 
      randomCompositeASTTermsForTag("Assign", subs2x, 3, 30);

    Vector sqsym = new Vector(); 
    sqsym.add(new ASTSymbolTerm(";")); 

    Vector subsseq = new Vector(); 
    subsseq.add(assgns2); 
    subsseq.add(sqsym); 
    subsseq.add(assgns2); 

    Vector sqs = 
      randomCompositeASTTermsForTag("Seq", subsseq, 3, 100);

    Vector ifsym = new Vector(); 
    ifsym.add(new ASTSymbolTerm("if"));

    Vector thensym = new Vector(); 
    thensym.add(new ASTSymbolTerm("then"));

    Vector elsesym = new Vector(); 
    elsesym.add(new ASTSymbolTerm("else"));

    Vector endifsym = new Vector(); 
    endifsym.add(new ASTSymbolTerm("endif"));

    Vector subifs = new Vector(); 
    subifs.add(ifsym); 
    subifs.add(cmps); 
    subifs.add(thensym); 
    subifs.add(assgns2); 
    subifs.add(elsesym); 
    subifs.add(assgns); 
    subifs.add(endifsym); 

    Vector simpleifs = 
      randomCompositeASTTermsForTag("If", subifs, 7, 40);
    // size 25

    Vector subs3 = new Vector(); 
    subs3.add(assgns); 
    subs3.add(sqsym); 
    subs3.add(assgns); 

    Vector sqs0 = 
      randomCompositeASTTermsForTag("Seq", subs3, 3, 30);
    // size 11

    Vector subs3x = new Vector(); 
    subs3x.add(assgns); 
    subs3x.add(sqsym); 
    subs3x.add(sqs0); 

    Vector sqs1x = 
      randomCompositeASTTermsForTag("Seq", subs3x, 3, 100);
    // size 17

  
    Vector subs4 = new Vector(); 
    subs4.add(ifsym); 
    subs4.add(cmps); 
    subs4.add(thensym); 
    // subs4.add(sqs1x);
    subs4.add(simpleifs);  
    subs4.add(elsesym); 
    // subs4.add(sqs1x); 
    subs4.add(simpleifs); 
    subs4.add(endifsym); 

    Vector sqs1 = 
      randomCompositeASTTermsForTag("If", subs4, 7, 100);
  
   
    // System.out.println(sqs1); 
    // Size 23 for assgns; Size 33 for sqs0; 45 with sqs1
    // Size 60 with simpleifs

    Vector subs4x = new Vector(); 
    subs4x.add(ifsym); 
    subs4x.add(cmps); 
    subs4x.add(thensym); 
    subs4x.add(simpleifs); 
    subs4x.add(elsesym); 
    subs4x.add(assgns); 
    subs4x.add(endifsym); 

    Vector sqs4x = 
      randomCompositeASTTermsForTag("If", subs4x, 7, 100);
    // size 61 or 41 with simpleifs
    
   
    Vector forsym = new Vector(); 
    forsym.add(new ASTSymbolTerm("for"));
    Vector eqsym = new Vector(); 
    eqsym.add(new ASTSymbolTerm("="));
    Vector semisym = new Vector(); 
    semisym.add(new ASTSymbolTerm(";"));
    Vector dosym = new Vector(); 
    dosym.add(new ASTSymbolTerm("do"));
    Vector endforsym = new Vector(); 
    endforsym.add(new ASTSymbolTerm("endfor"));

    Vector subs5 = new Vector(); 
    subs5.add(forsym); 
    subs5.add(vars); 
    subs5.add(eqsym); 
    subs5.add(consts); 
    subs5.add(semisym); 
    subs5.add(cmps); 
    subs5.add(semisym); 
    subs5.add(exprs); 
    subs5.add(dosym); 
    // subs5.add(assgns);
    subs5.add(simpleifs);  
    subs5.add(endforsym); 

    Vector fors = 
      randomCompositeASTTermsForTag("For", subs5, 11, 100);
    // Each of size 22 with assgns in do body; 
    // 42 with simpleifs
         


    File chtml = new File("output/asts.txt"); 
    try
    { PrintWriter chout = new PrintWriter(
                              new BufferedWriter(
                                new FileWriter(chtml)));
      for (int i = 0; i < sqs1.size(); i++)
      { ASTTerm oclexample = (ASTTerm) sqs1.get(i); 
        chout.println(oclexample); 
      }
      chout.close(); 
    
    } 
    catch (Exception _fex) 
    { System.err.println("! No file: output/asts.txt"); }  

  } */ 



/* tree2tree dataset format: */ 

/* {"target_ast": 
      {"root": "<LET>", 
       "children": 
          [{"root": "blank", 
              "children": []}, 
           {"root": "<IF>", 
              "children": 
                [{"root": "<CMP>", 
                  "children": 
                     [{"root": "<Expr>", 
                       "children": 
                          [{"root": "0", 
                               "children": []}]
                      }, 
                      {"root": ">", 
                          "children": []}, 
                      {"root": "<Expr>", 
                          "children": 
                            [{"root": "x", 
                              "children": []}]
                      }
                     ]
                 }, 

                 {"root": "<LET>", 
                     "children": 
                        [{"root": "y", 
                             "children": []}, 
                         {"root": "<Expr>", 
                          "children": 
                             [{"root": "y", "children": []}]
                         }, 
                         {"root": "<UNIT>", "children": []}
                        ]
                 }, 
                 {"root": "<LET>", 
                     "children": 
                        [{"root": "y", "children": []}, 
                         {"root": "<Expr>", 
                             "children": 
                                [{"root": "y", 
                                  "children": []}]
                         }, 
                         {"root": "<UNIT>", 
                             "children": []}
                        ]
                 }
               ]}, 
             {"root": "<LET>", "children": [{"root": "x", "children": []}, {"root": "<Expr>", "children": [{"root": "1", "children": []}]}, {"root": "<LET>", "children": [{"root": "y", "children": []}, {"root": "<Expr>", "children": [{"root": "1", "children": []}]}, {"root": "<UNIT>", "children": []}]}]}]}, 

"source_prog": ["if", "0", ">", "x", "then", "y", "=", "y", "else", "y", "=", "y", "endif", ";", "x", "=", "1", ";", "y", "=", "1"], 

"source_ast": {"root": "<SEQ>", "children": [{"root": "<IF>", "children": [{"root": "<CMP>", "children": [{"root": "<Expr>", "children": [{"root": "0", "children": []}]}, {"root": ">", "children": []}, {"root": "<Expr>", "children": [{"root": "x", "children": []}]}]}, {"root": "<ASSIGN>", "children": [{"root": "y", "children": []}, {"root": "<Expr>", "children": [{"root": "y", "children": []}]}]}, {"root": "<ASSIGN>", "children": [{"root": "y", "children": []}, {"root": "<Expr>", "children": [{"root": "y", "children": []}]}]}]}, {"root": "<SEQ>", "children": [{"root": "<ASSIGN>", "children": [{"root": "x", "children": []}, {"root": "<Expr>", "children": [{"root": "1", "children": []}]}]}, {"root": "<ASSIGN>", "children": [{"root": "y", "children": []}, {"root": "<Expr>", "children": [{"root": "1", "children": []}]}]}]}]}, 

"target_prog": ["let", "blank", "=", "if", "0", ">", "x", "then", "let", "y", "=", "y", "in", "()", "else", "let", "y", "=", "y", "in", "()", "in", "let", "x", "=", "1", "in", "let", "y", "=", "1", "in", "()"]} */ 

/* Raw format: 
{"for_tree": 
   ["<SEQ>", ["<ASSIGN>", "y", ["<Expr>", "y"]], 
             ["<SEQ>", 
                ["<IF>", 
                   ["<CMP>", ["<Expr>", 1], "<", ["<Expr>", 0]], 
                   ["<ASSIGN>", "x", ["<Expr>", 0]], 
                   ["<ASSIGN>", "x", ["<Expr>", 0]]
                ], 
                ["<ASSIGN>", "y", ["<Expr>", 1]]
              ]
    ], 

  "raw_for": "y = y ; if 1 < 0 then x = 0 else x = 0 endif ; y = 1", "raw_lam": "let y = y in let blank = if 1 < 0 then let x = 0 in () else let x = 0 in () in let y = 1 in ()", 

  "lam_tree": 
     ["<LET>", "y", 
        ["<Expr>", "y"], 
        ["<LET>", "blank", 
            ["<IF>", 
                ["<CMP>", ["<Expr>", 1], "<", ["<Expr>", 0]], 
                ["<LET>", "x", 
                        ["<Expr>", 0], "<UNIT>"], 
                        ["<LET>", "x", ["<Expr>", 0], "<UNIT>"]
                 ], 
                 ["<LET>", "y", ["<Expr>", 1], "<UNIT>"]
             ]
      ]
   } */ 

