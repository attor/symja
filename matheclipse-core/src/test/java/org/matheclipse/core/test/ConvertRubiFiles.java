package org.matheclipse.core.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

import org.matheclipse.basic.Config;
import org.matheclipse.core.convert.AST2Expr;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IAST;
import org.matheclipse.core.interfaces.IExpr;
import org.matheclipse.core.interfaces.ISymbol;
import org.matheclipse.parser.client.Parser;
import org.matheclipse.parser.client.ast.ASTNode;

/**
 * Convert the Rubi files from <a href="http://www.apmaths.uwo.ca/~arich/">Rubi
 * - Indefinite Integration Reduction Rules</a>
 * 
 */
public class ConvertRubiFiles {
	private static int NUMBER_OF_RULES_PER_FILE = 100;

	public static List<ASTNode> parseFileToList(String fileName) {
		try {
			File file = new File(fileName);
			final BufferedReader f = new BufferedReader(new FileReader(file));
			final StringBuffer buff = new StringBuffer(1024);
			String line;
			while ((line = f.readLine()) != null) {
				buff.append(line);
				buff.append('\n');
			}
			f.close();
			String inputString = buff.toString();
			Parser p = new Parser(false, true);
			return p.parseList(inputString);
			// assertEquals(obj.toString(),
			// "Plus[Plus[Times[-1, a], Times[-1, Times[b, Factorial2[c]]]], d]");
		} catch (Exception e) {
			e.printStackTrace();
			// assertEquals("", e.getMessage());
		}
		return null;
	}

	public static void writeFile(String fileName, StringBuffer buffer) {
		try {
			File file = new File(fileName);
			final BufferedWriter f = new BufferedWriter(new FileWriter(file));
			final StringBuffer buff = new StringBuffer(1024);
			f.append(buffer);
			f.close();
		} catch (Exception e) {
			e.printStackTrace();
			// assertEquals("", e.getMessage());
		}
	}

	public static void convert(ASTNode node, StringBuffer buffer, boolean last) {
		try {
			// convert ASTNode to an IExpr node

			IExpr expr = AST2Expr.CONST.convert(node);

			// TODO allow Module in future versions
			ISymbol module = F.symbol("Module");
			if (expr.isFree(module, true)) {
				if (expr.isAST(F.SetDelayed, 3)) {
					IAST ast = (IAST) expr;
					buffer.append("SetDelayed(");
					buffer.append(ast.get(1).internalFormString(true, 0));
					buffer.append(",\n    ");
					buffer.append(ast.get(2).internalFormString(true, 0));
					if (last) {
						buffer.append(")\n");
					} else {
						buffer.append("),\n");
					}
				} else if (expr.isAST(F.If, 4)) {
					IAST ast = (IAST) expr;
					// if (ast.get(1).toString().equals("ShowSteps")) {
					expr = ast.get(3);
					if (expr.isAST(F.SetDelayed, 3)) {
						ast = (IAST) expr;
						buffer.append("SetDelayed(");
						buffer.append(ast.get(1).internalFormString(true, 0));
						buffer.append(",\n    ");
						buffer.append(ast.get(2).internalFormString(true, 0));
						if (last) {
							buffer.append(")\n");
						} else {
							buffer.append("),\n");
						}
					}
					// }
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		F.initSymbols(null, null, false);
		Config.SERVER_MODE = false;

		String[] fileNames = {
		// "C:\\temp\\RationalFunctionIntegrationRules.m",
		// "C:\\temp\\AlgebraicFunctionIntegrationRules.m",
		// "C:\\temp\\ExponentialFunctionIntegrationRules.m",
		"C:\\temp\\Rubi\\TrigFunctionIntegrationRules.m",
		// "C:\\temp\\HyperbolicFunctionIntegrationRules.m",
		// "C:\\temp\\LogarithmFunctionIntegrationRules.m",
		// "C:\\temp\\InverseTrigFunctionIntegrationRules.m",
		// "C:\\temp\\InverseHyperbolicFunctionIntegrationRules.m",
		// "C:\\temp\\ErrorFunctionIntegrationRules.m",
		// "C:\\temp\\IntegralFunctionIntegrationRules.m",
		// "C:\\temp\\SpecialFunctionIntegrationRules.m",
		// "C:\\temp\\GeneralIntegrationRules.m"
		};
		for (int i = 0; i < fileNames.length; i++) {
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>");
			System.out.println(">>>>> File name: " + fileNames[i]);
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>");

			StringBuffer buffer = new StringBuffer(100000);
			List<ASTNode> list = parseFileToList(fileNames[i]);
			if (list != null) {
				int cnt = 0;
				int fcnt = 0;

				for (int j = 0; j < list.size(); j++) {
					if (cnt == 0) {
						buffer = new StringBuffer(100000);
						buffer.append("public static IAST RULES = List( \n");
					}
					ASTNode astNode = list.get(j);
					cnt++;
					convert(astNode, buffer, cnt == NUMBER_OF_RULES_PER_FILE);

					if (cnt == NUMBER_OF_RULES_PER_FILE) {
						buffer.append(");");
						writeFile("C:\\temp\\Rubi\\TrigFunctionIntegrationRules" + fcnt + ".java", buffer);
						fcnt++;
						cnt = 0;
					}
				}
				if (cnt != 0) {
					// System.out.println(");");
					buffer.append(");");
					writeFile("C:\\temp\\Rubi\\TrigFunctionIntegrationRules" + fcnt + ".java", buffer);
				}
				System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>");
				System.out.println(">>>>> Number of entries: " + list.size());
				System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>");
			}
		}
	}
}