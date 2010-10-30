package org.matheclipse.core.reflection.system;

import org.matheclipse.core.eval.exception.Validate;
import org.matheclipse.core.eval.interfaces.IFunctionEvaluator;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IAST;
import org.matheclipse.core.interfaces.IExpr;
import org.matheclipse.core.interfaces.INumber;
import org.matheclipse.core.interfaces.ISignedNumber;
import org.matheclipse.core.interfaces.ISymbol;

public class NonNegative implements IFunctionEvaluator {

	public NonNegative() {
	}

	public IExpr evaluate(final IAST ast) {
		Validate.checkSize(ast, 2);
		if (ast.get(1) instanceof ISignedNumber) {
			if (!((ISignedNumber) ast.get(1)).isNegative()) {
				return F.True;
			}
			return F.False;
		}
		if (ast.get(1) instanceof INumber) {
			return F.False;
		}
		return null;
	}

	public IExpr numericEval(final IAST functionList) {
		return evaluate(functionList);
	}

	public void setUp(final ISymbol symbol) {
		symbol.setAttributes(ISymbol.LISTABLE);
	}

}
