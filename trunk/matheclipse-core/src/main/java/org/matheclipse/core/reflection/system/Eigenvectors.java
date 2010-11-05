package org.matheclipse.core.reflection.system;

import org.apache.commons.math.linear.EigenDecompositionImpl;
import org.apache.commons.math.linear.FieldMatrix;
import org.apache.commons.math.linear.InvalidMatrixException;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;
import org.apache.commons.math.util.MathUtils;
import org.matheclipse.core.convert.Convert;
import org.matheclipse.core.eval.exception.WrappedException;
import org.matheclipse.core.eval.interfaces.AbstractMatrix1Expr;
import org.matheclipse.core.expression.ExprFieldElement;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.interfaces.IAST;
import org.matheclipse.core.interfaces.IExpr;

/**
 * Compute the numerical Eigenvectors of a real symmetric matrix
 * 
 * See: <a
 * href="http://en.wikipedia.org/wiki/Eigenvalue,_eigenvector_and_eigenspace"
 * >Eigenvalue, eigenvector and eigenspace</a>
 */
public class Eigenvectors extends AbstractMatrix1Expr {

	public Eigenvectors() {
		super();
	}

	@Override
	public IExpr evaluate(final IAST function) {
		// switch to numeric calculation
		return numericEval(function);
	}

	public IAST realMatrixEval(RealMatrix matrix) {
		try {
			IAST list = F.List();
			EigenDecompositionImpl ed = new EigenDecompositionImpl(matrix, MathUtils.SAFE_MIN);
			for (int i = 0; i < matrix.getColumnDimension(); i++) {
				RealVector rv = ed.getEigenvector(i);
				list.add(Convert.realVector2List(rv));
			}
			return list;
		} catch (InvalidMatrixException ime) {
			throw new WrappedException(ime);
		}
	}

	@Override
	public ExprFieldElement matrixEval(FieldMatrix<ExprFieldElement> matrix) {
		// TODO Auto-generated method stub
		return null;
	}
}