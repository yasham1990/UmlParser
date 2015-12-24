import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class UmlParser extends JavaBaseListener {

	public static StringBuffer formattedString = new StringBuffer();
	public static Map<String, String> variableStoreMap = new HashMap<String, String>();
	public static Set<String> classCheckList = new HashSet<String>();
	public static Map<String, String> dependencyList = new HashMap<String, String>();
	public static List<String> dependencyWriteList = new ArrayList<String>();
	public static Map<String, String> classTypeMap = new HashMap<String, String>();
	public static Set<String> usesList = new HashSet<String>();
	public static List<String> checkGetterSetter = new ArrayList<String>();

	private final static String CLASSNAME = "className";
	private final static String VARIABLENAME = "variableName";
	private final static String VARIABLETYPE = "variableType";
	private final static String PREVIOUSVARIABLETYPE = "previousVariableType";
	private final static String VARIABLEMODIFIER = "variableModifier";
	private final static String DEPENDENTVARIABLEMODIFIER = "variableDependentModifier";
	private final static String DEPENDENTVARIABLE = "variableDependent";
	private final static String PREVIOUSDEPENDENTVARIABLEMODIFIER = "previouVariableDependentModifier";
	private final static String PREVIOUSDEPENDENTVARIABLE = "previousVariableDependent";
	private final static String ACCESSTYPE = "accessType";
	private final static String PREVIOUSACCESSTYPE = "previousAccessType";
	private final static String ISFIRST = "isFirst";
	private final static String METHODNAME = "methodName";
	private final static String METHODTYPE = "methodType";
	private final static String METHODPARAMETERTYPE = "methodParameterType";

	/*
	 * This method is used for getting the class for java file.
	 * 
	 * @see JavaBaseListener#enterClassName(JavaParser.ClassNameContext)
	 */
	public void enterClassName(JavaParser.ClassNameContext ctx) {
		String value = ctx.getText();
		classTypeMap.put(value, "class");
		variableStoreMap.put(CLASSNAME, value);
		formattedString.append("[" + value);
		customReset();
		createDependency(value);

	}

	/*
	 * This method is used for getting the interface for java file.
	 * 
	 * @see JavaBaseListener#enterInterfaceName(JavaParser.InterfaceNameContext)
	 */
	public void enterInterfaceName(JavaParser.InterfaceNameContext ctx) {
		String value = ctx.getText();
		dependencyWriteList = new ArrayList<String>();
		variableStoreMap.put(CLASSNAME, value);
		formattedString.append("[" + value);
		classTypeMap.put(value, "interface");
		customReset();
		createDependency(value);
	}

	/*
	 * This method is used for getting the inherited class name for java file.
	 * 
	 * @see JavaBaseListener#enterInheritedClassName(JavaParser.
	 * InheritedClassNameContext)
	 */
	public void enterInheritedClassName(JavaParser.InheritedClassNameContext ctx) {
		String value = ctx.getText();
		dependencyWriteList.add(",[" + variableStoreMap.get(CLASSNAME) + "]-^[" + value + "]");
	}

	/*
	 * This method is used for getting the implemented interface name for java
	 * file.
	 * 
	 * @see JavaBaseListener#enterInterfaceClassName(JavaParser.
	 * InterfaceClassNameContext)
	 */
	public void enterInterfaceClassName(JavaParser.InterfaceClassNameContext ctx) {
		String value = ctx.getText();
		List<String> interfaceNameList = Arrays.asList(value.split(","));
		for (String string : interfaceNameList)
			dependencyWriteList.add(",[" + variableStoreMap.get(CLASSNAME) + "]" + "-.-^[" + string + "]");
	}

	/*
	 * This method is used for checking the attribute declaration type (Array or
	 * single).
	 * 
	 * @see JavaBaseListener#enterFieldDeclaration(JavaParser.
	 * FieldDeclarationContext)
	 */
	public void enterFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
		String value = ctx.getText();
		if (value.contains("["))
			variableStoreMap.put(VARIABLEMODIFIER, "(*)");
		else if (value.contains("<"))
			variableStoreMap.put(VARIABLEMODIFIER, "(*)");
		else
			variableStoreMap.put(VARIABLEMODIFIER, "");
	}

	/*
	 * This method is used for getting modifier for attribute declaration.
	 * 
	 * @see JavaBaseListener#enterModifier(JavaParser.ModifierContext)
	 */
	public void enterModifier(JavaParser.ModifierContext ctx) {
		String value = ctx.getText();
		if (("public".equals(value) || "private".equals(value)) && ("public".equals(variableStoreMap.get(ACCESSTYPE))
				|| "private".equals(variableStoreMap.get(ACCESSTYPE))))
			variableStoreMap.put(ACCESSTYPE, "");
		String accessTypeValue = variableStoreMap.get(ACCESSTYPE) + (value != null ? value : "");
		variableStoreMap.put(ACCESSTYPE, accessTypeValue);
	}

	/*
	 * This method is used for getting type for attribute in the class.
	 * 
	 * @see JavaBaseListener#enterVariableType(JavaParser.VariableTypeContext)
	 */
	public void enterVariableType(JavaParser.VariableTypeContext ctx) {
		String typeOfVar = ctx.getText();
		if (typeOfVar.contains("["))
			typeOfVar = typeOfVar.substring(0, typeOfVar.indexOf("["));
		else if (typeOfVar.contains("<"))
			typeOfVar = typeOfVar.substring(typeOfVar.indexOf("<") + 1, typeOfVar.indexOf(">"));
		variableStoreMap.put(VARIABLETYPE, typeOfVar);
		String valueForMap = "[" + variableStoreMap.get(CLASSNAME) + "]"
				+ ("".equals(variableStoreMap.get(VARIABLEMODIFIER)) ? "-1>" : "-*>>") + "["
				+ variableStoreMap.get(VARIABLETYPE) + "]";
		if (classCheckList != null && classCheckList.contains(typeOfVar) && dependencyList != null
				&& !dependencyList.containsKey(variableStoreMap.get(CLASSNAME) + typeOfVar)) {
			dependencyList.put(variableStoreMap.get(CLASSNAME) + typeOfVar, valueForMap);
			dependencyList.put(typeOfVar + variableStoreMap.get(CLASSNAME), valueForMap);
			variableStoreMap.put(DEPENDENTVARIABLE, ctx.getText());
		} else if (classCheckList != null && classCheckList.contains(typeOfVar)) {
			variableStoreMap.put(DEPENDENTVARIABLEMODIFIER, "mappingDone");
		}
	}

	/*
	 * This method is used for getting name for attribute in the class.
	 * 
	 * @see JavaBaseListener#enterVariableName(JavaParser.VariableNameContext)
	 */
	public void enterVariableName(JavaParser.VariableNameContext ctx) {
		String value = ctx.getText();
		if ("".equals(variableStoreMap.get(VARIABLETYPE))) {
			variableStoreMap.put(DEPENDENTVARIABLEMODIFIER, variableStoreMap.get(PREVIOUSDEPENDENTVARIABLEMODIFIER));
			variableStoreMap.put(DEPENDENTVARIABLE, variableStoreMap.get(PREVIOUSDEPENDENTVARIABLE));
			variableStoreMap.put(ACCESSTYPE, variableStoreMap.get(PREVIOUSACCESSTYPE));
			variableStoreMap.put(VARIABLETYPE, variableStoreMap.get(PREVIOUSVARIABLETYPE));
		}
		variableStoreMap.put(VARIABLENAME, value);
		checkGetterSetter.add(("get" + variableStoreMap.get(VARIABLENAME)).toLowerCase());
		checkGetterSetter.add(("set" + variableStoreMap.get(VARIABLENAME)).toLowerCase());
		if (!"mappingDone".equals(variableStoreMap.get(DEPENDENTVARIABLEMODIFIER))) {
			if (!"".equals(variableStoreMap.get(DEPENDENTVARIABLE))) {

				dependencyWriteList.add(",[" + variableStoreMap.get(CLASSNAME) + "]"
						+ ("".equals(variableStoreMap.get(VARIABLEMODIFIER)) ? "-1>" : "-*>") + "["
						+ variableStoreMap.get(VARIABLETYPE) + "]");
			}

			else if (variableStoreMap != null && (variableStoreMap.get(ACCESSTYPE).contains("public")
					|| variableStoreMap.get(ACCESSTYPE).contains("private"))) {
				formattedString.append(("true".equals(variableStoreMap.get(ISFIRST)) ? "|" : "")
						+ (variableStoreMap.get(ACCESSTYPE).contains("private") ? "-" : "+")
						+ variableStoreMap.get(VARIABLENAME) + ":");
				formattedString.append(variableStoreMap.get(VARIABLETYPE));
				formattedString.append(variableStoreMap.get(VARIABLEMODIFIER));
				formattedString.append(";");
			}
		} else {
			String dependencyValue = dependencyList
					.get(variableStoreMap.get(CLASSNAME) + variableStoreMap.get(VARIABLETYPE));
			String associationType = (dependencyValue != null && dependencyValue.contains("1")) ? "1" : "*";
			dependencyWriteList.add(",[" + variableStoreMap.get(CLASSNAME) + "]" + associationType
					+ ("".equals(variableStoreMap.get(VARIABLEMODIFIER)) ? "-1" : "-*") + "["
					+ variableStoreMap.get(VARIABLETYPE) + "]");
			formattedString = new StringBuffer(formattedString.toString().replace(dependencyValue, ""));
		}
		customVariableNameReset();

	}

	/*
	 * This method is used for getting parameter type for methods in the class.
	 * 
	 * @see JavaBaseListener#enterMethodParametersType(JavaParser.
	 * MethodParametersTypeContext)
	 */
	public void enterMethodParametersType(JavaParser.MethodParametersTypeContext ctx) {
		variableStoreMap.put(METHODPARAMETERTYPE, ctx.getText());
	}

	/*
	 * This method is used for getting parameter type for methods in the
	 * interface.
	 * 
	 * @see JavaBaseListener#enterInterfaceMethodParametersType(JavaParser.
	 * InterfaceMethodParametersTypeContext)
	 */
	public void enterInterfaceMethodParametersType(JavaParser.InterfaceMethodParametersTypeContext ctx) {
		variableStoreMap.put(METHODPARAMETERTYPE, ctx.getText());
	}

	/*
	 * This method is used for getting parameter type for constructor in the
	 * class.
	 * 
	 * @see JavaBaseListener#enterConstructorMethodParameterType(JavaParser.
	 * ConstructorMethodParameterTypeContext)
	 */
	public void enterConstructorMethodParameterType(JavaParser.ConstructorMethodParameterTypeContext ctx) {
		variableStoreMap.put(METHODPARAMETERTYPE, ctx.getText());
	}

	/*
	 * This method is used for getting parameter name for methods in the class.
	 * 
	 * @see JavaBaseListener#enterMethodParametersTypeName(JavaParser.
	 * MethodParametersTypeNameContext)
	 */
	public void enterMethodParametersTypeName(JavaParser.MethodParametersTypeNameContext ctx) {
		updateParametersType(variableStoreMap.get(METHODPARAMETERTYPE), ctx.getText());
		variableStoreMap.put(ACCESSTYPE, "");
	}

	/*
	 * This method is used for getting parameter name for methods in the
	 * interface.
	 * 
	 * @see JavaBaseListener#enterInterfaceMethodParametersTypeName(JavaParser.
	 * InterfaceMethodParametersTypeNameContext)
	 */
	public void enterInterfaceMethodParametersTypeName(JavaParser.InterfaceMethodParametersTypeNameContext ctx) {
		updateParametersType(variableStoreMap.get(METHODPARAMETERTYPE), ctx.getText());
		variableStoreMap.put(ACCESSTYPE, "");
	}

	/*
	 * This method is used for getting parameter name for constructor in the
	 * class.
	 * 
	 * @see JavaBaseListener#enterConstructorMethodParameterTypeName(JavaParser.
	 * ConstructorMethodParameterTypeNameContext)
	 */
	public void enterConstructorMethodParameterTypeName(JavaParser.ConstructorMethodParameterTypeNameContext ctx) {
		updateParametersType(variableStoreMap.get(METHODPARAMETERTYPE), ctx.getText());
		variableStoreMap.put(ACCESSTYPE, "");

	}

	/*
	 * This method is used for getting type for methods in the class.
	 * 
	 * @see JavaBaseListener#enterMethodType(JavaParser.MethodTypeContext)
	 */
	public void enterMethodType(JavaParser.MethodTypeContext ctx) {
		updateMethodType(ctx.getText());
	}

	/*
	 * This method is used for getting type for methods in the interface.
	 * 
	 * @see JavaBaseListener#enterInterfaceMethodType(JavaParser.
	 * InterfaceMethodTypeContext)
	 */
	public void enterInterfaceMethodType(JavaParser.InterfaceMethodTypeContext ctx) {
		updateMethodType(ctx.getText());
	}

	/*
	 * This method is used for getting name for methods in the class.
	 * 
	 * @see JavaBaseListener#enterMethodName(JavaParser.MethodNameContext)
	 */
	public void enterMethodName(JavaParser.MethodNameContext ctx) {
		String value = ctx.getText();
		if (variableStoreMap != null && variableStoreMap.get(ACCESSTYPE).contains("public") && checkGetterSetter != null
				&& !checkGetterSetter.contains(value.toLowerCase())) {
			updateMethodName(value);
		} else if (checkGetterSetter != null && checkGetterSetter.contains(value.toLowerCase())) {
			checkGetterSetter.remove(value.toLowerCase());
			value = value.replace("set", "");
			value = value.replace("get", "");
			if (checkGetterSetter != null && (checkGetterSetter.contains(("set" + value).toLowerCase())
					|| checkGetterSetter.contains(("get" + value).toLowerCase())))
				updateMethodName(ctx.getText());
			else {
				deleteSetGetMethod(value);
			}
		}
	}

	/*
	 * This method is used for getting name for constructor in the class.
	 * 
	 * @see
	 * JavaBaseListener#enterConstructorName(JavaParser.ConstructorNameContext)
	 */
	public void enterConstructorName(JavaParser.ConstructorNameContext ctx) {
		if ("".equals(variableStoreMap.get(ISFIRST)))
			variableStoreMap.put(ISFIRST, "true");
		updateMethodName(ctx.getText());
	}

	/*
	 * This method is used for getting name for methods in the interface.
	 * 
	 * @see JavaBaseListener#enterInterfaceMethodName(JavaParser.
	 * InterfaceMethodNameContext)
	 */
	public void enterInterfaceMethodName(JavaParser.InterfaceMethodNameContext ctx) {
		String value = ctx.getText();
		updateMethodName(value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see JavaBaseListener#enterLocalVariableDeclarationType(JavaParser.
	 * LocalVariableDeclarationTypeContext)
	 */
	public void enterLocalVariableDeclarationType(JavaParser.LocalVariableDeclarationTypeContext ctx) {
		String value = ctx.getText();
		if (value != null && classCheckList != null && classCheckList.contains(value)) {
			if (!classTypeMap.isEmpty())
				dependencyWriteList.add(",[" + variableStoreMap.get(CLASSNAME) + "]uses-.->[" + value + "]");
		}
	}

	/*
	 * Main method for the execution of UmlParser.
	 */
	public static void main(String[] args) {
		URL url = null;
		URLConnection con = null;
		DataInputStream dis = null;
		FileOutputStream fos = null;
		byte[] fileData = null;
		FileInputStream stream = null;

		try {
			if (args.length>=2 && args[0] != null && args[1] != null) {
				String filePath = args[0];
				File file = new File(filePath);
				UmlParser umlParser = new UmlParser();
				umlParser.reset();
				String fileName = "";
				File[] files = file.listFiles();
				if (files != null) {
					for (int j = 0; j < files.length; j++) {
						fileName = files[j].getName();
						if (fileName != null && fileName.contains("java") && !fileName.contains("java~"))
							classCheckList.add(umlParser.getFileName(fileName));

					}
					for (int i = 0; i < files.length; i++) {
						fileName = files[i].getName();
						if (fileName != null && fileName.contains("java") && !fileName.contains("java~")) {
							classCheckList.remove(umlParser.getFileName(fileName));
							stream = new FileInputStream(files[i]);
							getUMLFormattedString(stream);
							classCheckList.add(umlParser.getFileName(fileName));
						}

					}
					formattedString.deleteCharAt(formattedString.length() - 1);

					Pattern p = Pattern.compile("\\[(.*?)\\]");
					Matcher m = p.matcher(formattedString != null ? formattedString : "");

					while (m.find()) {
						if (!m.group(1).contains("|") && !classCheckList.contains(m.group(1))) {
							formattedString = new StringBuffer(
									formattedString.toString().replace(("[" + m.group(1) + "]"), ""));
							formattedString = new StringBuffer(formattedString.toString().replace("|]", "]"));

						}
					}
					checkInterfaceType();
					url = new URL(
							"http://yuml.me/diagram/class/" + URLEncoder.encode(formattedString.toString(), "UTF-8")); // File
					con = url.openConnection(); // open the url connection.
					dis = new DataInputStream(con.getInputStream());
					fileData = new byte[con.getContentLength()];
					for (int q = 0; q < fileData.length; q++) {
						fileData[q] = dis.readByte();
					}
					dis.close(); // close the data input stream
					filePath = args[1];
					fos = new FileOutputStream(new File(filePath));
					fos.write(fileData);
					fos.close();
					System.out.println("Successfully executed. Please check class diagram image on the path provided.");
				} else {
					System.out.println("Please check the filepath provided.");
				}
			} else {
				System.out.println("Please enter the values for java souce code location and png file path for class diagram.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/*
	 * Replace all the interface name with interface syntax.
	 */
	public static void checkInterfaceType() {
		if (classTypeMap != null && !classTypeMap.isEmpty()) {
			Iterator it = classTypeMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry) it.next();
				if ("interface".equals(pair.getValue())) {
					formattedString = new StringBuffer(
							formattedString.toString().replace(("[" + pair.getKey()).toString() + "|",
									("[\uFF1C\uFF1CInterface\uFF1E\uFF1E" + pair.getKey() + "|")));
					formattedString = new StringBuffer(
							formattedString.toString().replace(("[" + pair.getKey()).toString() + "]",
									("[\uFF1C\uFF1CInterface\uFF1E\uFF1E" + pair.getKey() + "]")));
				}
			}
		}
	}

	/*
	 * This method is responsible for parsing the file and generating the
	 * formatted string for class diagram.
	 */
	private static void getUMLFormattedString(InputStream is) throws IOException, FileNotFoundException {
		JavaLexer lexer = new JavaLexer(new ANTLRInputStream(is));
		// Get a list of matched tokens
		CommonTokenStream tokens = new CommonTokenStream(lexer);

		// Pass the tokens to the parser
		JavaParser parser = new JavaParser(tokens);

		// Specify our entry point
		JavaParser.CompilationUnitContext sentenceContext = parser.compilationUnit();

		// Walk it and attach our listener
		ParseTreeWalker walker = new ParseTreeWalker();
		UmlParser listener = new UmlParser();
		walker.walk(listener, sentenceContext);
		if (!variableStoreMap.isEmpty()) {
			formattedString.append("]");
			if (!dependencyWriteList.isEmpty()) {
				for (int i = 0; i < dependencyWriteList.size(); i++)
					formattedString.append(dependencyWriteList.get(i) + ",");
			} else
				formattedString.append(",");
			formattedString.append("[" + variableStoreMap.get(CLASSNAME) + "],");
		}
		variableStoreMap.clear();
	}

	/*
	 * This method is used for resetting the field.
	 */
	public void reset() {
		variableStoreMap.put(VARIABLENAME, "");
		variableStoreMap.put(VARIABLETYPE, "");
		variableStoreMap.put(VARIABLEMODIFIER, "");
		variableStoreMap.put(DEPENDENTVARIABLEMODIFIER, "");
		variableStoreMap.put(DEPENDENTVARIABLE, "");
		variableStoreMap.put(ACCESSTYPE, "");
	}

	/*
	 * This method is used for resetting the field.
	 */
	public void customReset() {
		variableStoreMap.put(DEPENDENTVARIABLEMODIFIER, "");
		variableStoreMap.put(DEPENDENTVARIABLE, "");
		variableStoreMap.put(ACCESSTYPE, "");
		variableStoreMap.put(ISFIRST, "true");
		dependencyWriteList = new ArrayList<String>();
	}

	/*
	 * This method is used for resetting the field.
	 */
	public void customVariableNameReset() {
		variableStoreMap.put(PREVIOUSDEPENDENTVARIABLEMODIFIER, variableStoreMap.get(DEPENDENTVARIABLEMODIFIER));
		variableStoreMap.put(PREVIOUSDEPENDENTVARIABLE, variableStoreMap.get(DEPENDENTVARIABLE));
		variableStoreMap.put(PREVIOUSACCESSTYPE, variableStoreMap.get(ACCESSTYPE));
		variableStoreMap.put(PREVIOUSVARIABLETYPE, variableStoreMap.get(VARIABLETYPE));
		variableStoreMap.put(DEPENDENTVARIABLEMODIFIER, "");
		variableStoreMap.put(DEPENDENTVARIABLE, "");
		variableStoreMap.put(ISFIRST, "");
		variableStoreMap.put(ACCESSTYPE, "");
		variableStoreMap.put(VARIABLETYPE, "");
	}

	/*
	 * This method is used for getting the file name without extension.
	 */
	public String getFileName(String fileName) {
		String value = (fileName != null && fileName.length() > 0) ? fileName : "";
		value = value.substring(value.lastIndexOf("/") + 1, value.lastIndexOf("."));
		return value;
	}

	/*
	 * This method is used for creating dependency in the class.
	 */

	private void createDependency(String value) {
		if (usesList != null && !usesList.isEmpty()) {
			for (Iterator<String> iterator = usesList.iterator(); iterator.hasNext();) {
				String string = iterator.next();
				String[] parts = string.split(",");
				String part1 = parts[0];
				String part2 = parts[1];
				if (value.equals(part2)) {
					dependencyWriteList.add(",[" + part1 + "]uses-.->[" + value + "]");
					iterator.remove();
				}

			}
		}
	}

	/*
	 * This method is used for updating method name syntax in formatted string.
	 */
	private void updateMethodName(String value) {
		variableStoreMap.put(METHODNAME, value);
		formattedString.append(
				("true".equals(variableStoreMap.get(ISFIRST)) ? "|+" : "+") + variableStoreMap.get(METHODNAME) + "():");
		formattedString.append(variableStoreMap.get(METHODTYPE) != null ? variableStoreMap.get(METHODTYPE) : "void");
		formattedString.append(";");
		variableStoreMap.put(ISFIRST, "second");
	}

	/*
	 * This method is used for updating method type syntax in formatted string.
	 */
	private void updateMethodType(String typeOfVar) {
		if ("interface".equals(classTypeMap.get(variableStoreMap.get(CLASSNAME)))
				|| variableStoreMap.get(ACCESSTYPE).contains("public")) {
			variableStoreMap.put(METHODTYPE, typeOfVar);
			if ("".equals(variableStoreMap.get(ISFIRST)))
				variableStoreMap.put(ISFIRST, "true");
		}
	}

	/*
	 * This method is used for updating method parameter type syntax in
	 * formatted string.
	 */
	private void updateParametersType(String value, String parameterName) {
		if (!"".equals(variableStoreMap.get(METHODNAME))
				&& ("interface".equals(classTypeMap.get(variableStoreMap.get(CLASSNAME)))
						|| variableStoreMap.get(ACCESSTYPE).contains("public"))) {
			int index = formattedString.lastIndexOf("):");
			if (index != -1)
				formattedString = new StringBuffer(formattedString.substring(0, index));
			Character lastVariable = formattedString.charAt(formattedString.length() - 1);
			String modifiedValue = value;
			Pattern p = Pattern.compile("\\[(.*?)\\]");
			Matcher m = p.matcher(value);
			while (m.find()) {
				modifiedValue = value.replace(m.group(1), "");
				modifiedValue = modifiedValue.replace("[]", "(*)");
			}
			
			p = Pattern.compile("\\[(.*?)\\]");
			m = p.matcher(parameterName);
			while (m.find()) {
				parameterName = parameterName.replace(m.group(1), "");
				parameterName = parameterName.replace("[]", "(*)");
			}
			
			formattedString.append(("(".equals(lastVariable.toString()) ? parameterName : "\u201A" + parameterName)
					+ ":" + modifiedValue + "):"
					+ (variableStoreMap.get(METHODTYPE) != null ? variableStoreMap.get(METHODTYPE) : "void") + ";");
			String className = variableStoreMap.get(CLASSNAME);
			modifiedValue = modifiedValue.replace("(*)", "");
			value = modifiedValue;
			if (value != null && !value.equals(className) && dependencyList != null && classCheckList != null
					&& classCheckList.contains(value)) {
				if (!classTypeMap.isEmpty()) {
					boolean isDone = false;
					for (String element : classTypeMap.keySet()) {
						if (value.equals(element)) {
							isDone = true;
							dependencyWriteList.add(",[" + className + "]uses-.->[" + value + "]");
						}

					}

					if (!isDone)
						usesList.add(className + "," + value);
				} else
					usesList.add(className + "," + value);
			}
		}
	}

	/*
	 * This method is used for deleting getter setter method..
	 */
	private void deleteSetGetMethod(String value) {
		int index = 0;
		value = value.replace("set", "");
		value = value.replace("get", "");
		if (formattedString != null && formattedString.toString().toLowerCase().contains(value.toLowerCase())) {
			index = formattedString.toString().indexOf(value.toLowerCase()) - 1;
			formattedString.setCharAt(index, '+');
		}

		Pattern p = Pattern.compile("get" + value + "\\((.*?)\\;");
		Matcher m = p.matcher(formattedString.toString());

		while (m.find()) {
			String removeString = "+get" + value + "(" + m.group(1) + ";]";
			formattedString = new StringBuffer(formattedString.toString().replace(removeString, ""));
			removeString = "+get" + value + "(" + m.group(1) + ";";
			formattedString = new StringBuffer(formattedString.toString().replace(removeString, ""));
		}

		p = Pattern.compile("set" + value + "\\((.*?)\\;");
		m = p.matcher(formattedString.toString());

		while (m.find()) {
			String removeString = "+set" + value + "(" + m.group(1) + ";]";
			formattedString = new StringBuffer(formattedString.toString().replace(removeString, ""));
			removeString = "+set" + value + "(" + m.group(1) + ";";
			formattedString = new StringBuffer(formattedString.toString().replace(removeString, ""));
		}

		variableStoreMap.put(METHODNAME, "");
	}
}
