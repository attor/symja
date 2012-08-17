package org.matheclipse.core.convert;

import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IAST;
import org.matheclipse.core.interfaces.IExpr;
import org.matheclipse.core.interfaces.IInteger;
import org.matheclipse.core.interfaces.ISymbol;
import org.matheclipse.core.reflection.system.Blank;
import org.matheclipse.core.reflection.system.Complex;
import org.matheclipse.core.reflection.system.Pattern;
import org.matheclipse.core.reflection.system.Rational;
import org.matheclipse.parser.client.ast.ASTNode;
import org.matheclipse.parser.client.ast.FloatNode;
import org.matheclipse.parser.client.ast.FractionNode;
import org.matheclipse.parser.client.ast.FunctionNode;
import org.matheclipse.parser.client.ast.IntegerNode;
import org.matheclipse.parser.client.ast.Pattern2Node;
import org.matheclipse.parser.client.ast.Pattern3Node;
import org.matheclipse.parser.client.ast.PatternNode;
import org.matheclipse.parser.client.ast.StringNode;
import org.matheclipse.parser.client.ast.SymbolNode;

/**
 * Converts a parsed <code>org.matheclipse.parser.client.ast.ASTNode</code>
 * expression into an IExpr expression
 * 
 */
public class AST2Expr { 

	/**
	 * Typical instance of an ASTNode to IExpr converter
	 */
	public final static AST2Expr CONST = new AST2Expr(ASTNode.class, IExpr.class);

	public AST2Expr(final Class<ASTNode> sType, final Class<IExpr> tType) {
		super();
		// super(sType, tType);
	}

	/**
	 * Converts a parsed FunctionNode expression into an IAST expression
	 */
	public IAST convert(IAST ast, FunctionNode functionNode) throws ConversionException {
		ast.set(0, convert(functionNode.get(0)));
		for (int i = 1; i < functionNode.size(); i++) {
			ast.add(convert(functionNode.get(i)));
		}
		return ast;
	}

	/**
	 * Converts a parsed ASTNode expression into an IExpr expression
	 */
	public IExpr convert(ASTNode node) throws ConversionException {
		if (node == null) {
			return null;
		}
		if (node instanceof Pattern2Node) {
			throw new UnsupportedOperationException("'__' pattern-matching expression not implemented");
		}
		if (node instanceof Pattern3Node) {
			throw new UnsupportedOperationException("'___' pattern-matching expression not implemented");
		}
		if (node instanceof FunctionNode) {
			final FunctionNode functionNode = (FunctionNode) node;
			final IAST ast = F.ast(convert(functionNode.get(0)), functionNode.size(), false);
			for (int i = 1; i < functionNode.size(); i++) {
				ast.add(convert(functionNode.get(i)));
			}
			IExpr head = ast.head();
			if (ast.isASTSizeGE(F.GreaterEqual, 3)) {
				ISymbol compareHead = F.Greater;
				return rewriteLessGreaterAST(ast, compareHead);
			} else if (ast.isASTSizeGE(F.Greater, 3)) {
				ISymbol compareHead = F.GreaterEqual;
				return rewriteLessGreaterAST(ast, compareHead);
			} else if (ast.isASTSizeGE(F.LessEqual, 3)) {
				ISymbol compareHead = F.Less;
				return rewriteLessGreaterAST(ast, compareHead);
			} else if (ast.isASTSizeGE(F.Less, 3)) {
				ISymbol compareHead = F.LessEqual;
				return rewriteLessGreaterAST(ast, compareHead);
			} else if (head.equals(F.PatternHead)) {
				final IExpr expr = Pattern.CONST.evaluate(ast);
				if (expr != null) {
					return expr;
				}
			} else if (head.equals(F.BlankHead)) {
				final IExpr expr = Blank.CONST.evaluate(ast);
				if (expr != null) {
					return expr;
				}
			} else if (head.equals(F.ComplexHead)) {
				final IExpr expr = Complex.CONST.evaluate(ast);
				if (expr != null) {
					return expr;
				}
			} else if (head.equals(F.RationalHead)) {
				final IExpr expr = Rational.CONST.evaluate(ast);
				if (expr != null) {
					return expr;
				}
			}
			return ast;
		}
		if (node instanceof SymbolNode) {
			if (node.getString().equals("I")) {
				// special - convert on input
				return F.CI;
			} else if (node.getString().equals("Infinity")) {
				// special - convert on input
				return F.CInfinity;
			}
			return F.$s(node.getString());
		}
		if (node instanceof PatternNode) {
			final PatternNode pn = (PatternNode) node;
			return F.$p((ISymbol) convert(pn.getSymbol()), convert(pn.getConstraint()), pn.isDefault());
		}
		if (node instanceof IntegerNode) {
			final IntegerNode integerNode = (IntegerNode) node;
			final String iStr = integerNode.getString();
			if (iStr != null) {
				return F.integer(iStr, integerNode.getNumberFormat());
			}
			return F.integer(integerNode.getIntValue());
		}
		if (node instanceof FractionNode) {
			FractionNode fr = (FractionNode) node;
			if (fr.isSign()) {
				return F.fraction((IInteger) convert(fr.getNumerator()), (IInteger) convert(fr.getDenominator())).negate();
			}
			return F.fraction((IInteger) convert(((FractionNode) node).getNumerator()), (IInteger) convert(((FractionNode) node)
					.getDenominator()));
		}
		if (node instanceof StringNode) {
			return F.stringx(node.getString());
		}
		if (node instanceof FloatNode) {
			return F.num(node.getString());
		}

		return F.$s(node.toString());
	}

	/**
	 * Convert less or greter relations on input. Example: convert expressions
	 * like <code>a<b<=c</code> to <code>Less[a,b]&&LessEqual[b,c]</code>.
	 * 
	 * @param ast
	 * @param compareHead
	 * @return
	 */
	private IExpr rewriteLessGreaterAST(final IAST ast, ISymbol compareHead) {
		IExpr temp;
		boolean evaled = false;
		IAST andAST = F.ast(F.And);
		for (int i = 1; i < ast.size(); i++) {
			temp = ast.get(i);
			if (temp.isASTSizeGE(compareHead, 3)) {
				IAST lt = (IAST) temp;
				andAST.add(lt);
				ast.set(i, lt.get(lt.size() - 1));
				evaled = true;
			}
		}
		if (evaled) {
			andAST.add(ast);
			return andAST;
		} else {
			return ast;
		}
	}
}